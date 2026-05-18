package com.resumeai.export.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.SolidBorder;
import com.resumeai.export.dto.ExportJobResponse;
import com.resumeai.export.dto.ExportRequest;
import com.resumeai.export.dto.ExportCompletedEvent;
import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.repository.ExportRepository;
import com.resumeai.export.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final ExportRepository exportRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;

    @Value("${rabbitmq.routing.export-completed}")
    private String exportCompletedRoutingKey;

    @Value("${services.resume-url}")
    private String resumeUrl;

    @Value("${services.section-url}")
    private String sectionUrl;

    @Value("${export.storage.local-path}")
    private String localStoragePath;

    private static final int FREE_DAILY_PDF_LIMIT = 10;
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(37, 99, 235);   // blue-600
    private static final DeviceRgb SECTION_BG    = new DeviceRgb(239, 246, 255); // blue-50
    private static final DeviceRgb TEXT_MUTED    = new DeviceRgb(107, 114, 128); // gray-500

    // ── Submit / status / delete ──────────────────────────────────────────────

    @Override
    @Transactional
    public ExportJobResponse submitPdfExport(ExportRequest request) {
        checkDailyLimit(request.getUserId());
        ExportJob job = createJob(request, ExportJob.Format.PDF);
        processExportAsync(job.getJobId());
        return toResponse(job);
    }

    @Override
    @Transactional
    public ExportJobResponse submitDocxExport(ExportRequest request) {
        ExportJob job = createJob(request, ExportJob.Format.DOCX);
        processExportAsync(job.getJobId());
        return toResponse(job);
    }

    @Override
    @Transactional
    public ExportJobResponse submitJsonExport(ExportRequest request) {
        ExportJob job = createJob(request, ExportJob.Format.JSON);
        processExportAsync(job.getJobId());
        return toResponse(job);
    }

    @Override
    public ExportJobResponse getJobStatus(String jobId) {
        return toResponse(findOrThrow(jobId));
    }

    @Override
    public List<ExportJobResponse> getExportsByUser(String userId) {
        return exportRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteExport(String jobId) {
        exportRepository.delete(findOrThrow(jobId));
    }

    // ── Async processing ──────────────────────────────────────────────────────

    @Async
    public void processExportAsync(String jobId) {
        ExportJob job = exportRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(ExportJob.Status.PROCESSING);
            exportRepository.save(job);

            // Fetch resume metadata
            Map resumeMeta = fetchResumeMeta(job.getResumeId());
            // Fetch all sections for the resume
            List<Map> sections = fetchSections(job.getResumeId());

            String filePath = generateFile(job, resumeMeta, sections);

            job.setStatus(ExportJob.Status.COMPLETED);
            job.setFileUrl(filePath);
            job.setCompletedAt(LocalDateTime.now());
            job.setFileSizeKb(getFileSizeKb(filePath));
            exportRepository.save(job);
            log.info("Export job {} completed: {}", jobId, filePath);

            // Publish event to RabbitMQ
            ExportCompletedEvent event = ExportCompletedEvent.builder()
                    .jobId(job.getJobId())
                    .userId(job.getUserId())
                    .fileUrl(job.getFileUrl())
                    .format(job.getFormat().name())
                    .completedAt(job.getCompletedAt())
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, exportCompletedRoutingKey, event);
            log.info("Published ExportCompletedEvent for job {}", jobId);

        } catch (Exception e) {
            log.error("Export job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(ExportJob.Status.FAILED);
            exportRepository.save(job);
        }
    }

    // ── Data fetching ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map fetchResumeMeta(String resumeId) {
        try {
            Map r = restTemplate.getForObject(resumeUrl + "/resumes/" + resumeId, Map.class);
            return r != null ? r : new HashMap<>();
        } catch (Exception e) {
            log.warn("Could not fetch resume {}: {}", resumeId, e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map> fetchSections(String resumeId) {
        try {
            List result = restTemplate.getForObject(
                    sectionUrl + "/sections/resume/" + resumeId, List.class);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch sections for resume {}: {}", resumeId, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── File generation dispatch ──────────────────────────────────────────────

    private String generateFile(ExportJob job, Map resumeMeta, List<Map> sections) throws Exception {
        File dir = new File(localStoragePath);
        if (!dir.exists()) dir.mkdirs();

        String fileName = job.getJobId() + "." + job.getFormat().name().toLowerCase();
        String filePath = localStoragePath + "/" + fileName;

        switch (job.getFormat()) {
            case JSON -> generateJsonFile(filePath, resumeMeta, sections);
            case PDF  -> generatePdfFile(filePath, resumeMeta, sections);
            case DOCX -> generateDocxFile(filePath, resumeMeta, sections);
        }
        return filePath;
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    private void generateJsonFile(String filePath, Map resumeMeta, List<Map> sections) throws Exception {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("resume", resumeMeta);
        output.put("sections", sections);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.writeValue(new File(filePath), output);
    }

    // ── PDF (iText 7 — structured, professional layout) ──────────────────────

    @SuppressWarnings("unchecked")
    private void generatePdfFile(String filePath, Map resumeMeta, List<Map> sections) throws Exception {
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(36, 50, 36, 50);

        PdfFont boldFont   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        String fullName = getStr(resumeMeta, "candidateName", "Your Name");
        String jobTitle = getStr(resumeMeta, "targetJobTitle", "");
        String email    = getStr(resumeMeta, "email", "");
        String phone    = getStr(resumeMeta, "phone", "");
        String location = getStr(resumeMeta, "location", "");

        // ─── Header: Name ────────────────────────────────────────────────────
        Paragraph namePara = new Paragraph(fullName)
                .setFont(boldFont)
                .setFontSize(22)
                .setFontColor(ACCENT_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);
        doc.add(namePara);

        // ─── Header: Job title ───────────────────────────────────────────────
        if (!jobTitle.isBlank()) {
            doc.add(new Paragraph(jobTitle)
                    .setFont(regularFont)
                    .setFontSize(12)
                    .setFontColor(TEXT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(4));
        }

        // ─── Contact line ─────────────────────────────────────────────────────
        List<String> contactParts = new ArrayList<>();
        if (!email.isBlank())    contactParts.add(email);
        if (!phone.isBlank())    contactParts.add(phone);
        if (!location.isBlank()) contactParts.add(location);
        if (!contactParts.isEmpty()) {
            doc.add(new Paragraph(String.join("  |  ", contactParts))
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setFontColor(TEXT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(14));
        }

        // ─── Sections ─────────────────────────────────────────────────────────
        // Sort by displayOrder
        sections.sort((a, b) -> {
            int oa = a.get("displayOrder") instanceof Number n ? n.intValue() : 99;
            int ob = b.get("displayOrder") instanceof Number n ? n.intValue() : 99;
            return Integer.compare(oa, ob);
        });

        for (Map section : sections) {
            Boolean visible = (Boolean) section.getOrDefault("visible", true);
            if (Boolean.FALSE.equals(visible)) continue;

            String title   = getStr(section, "title", getStr(section, "sectionType", "Section"));
            String content = getStr(section, "content", "");
            if (content.isBlank()) continue;

            // Section heading with accent underline
            doc.add(new Paragraph(title.toUpperCase())
                    .setFont(boldFont)
                    .setFontSize(10)
                    .setFontColor(ACCENT_COLOR)
                    .setMarginBottom(2));

            // Thin accent divider
            com.itextpdf.kernel.pdf.canvas.draw.SolidLine line =
                    new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f);
            line.setColor(ACCENT_COLOR);
            doc.add(new LineSeparator(line).setMarginBottom(4));

            // Content paragraphs — split by line breaks for bullet handling
            String[] lines = content.split("\\n");
            for (String l : lines) {
                String trimmed = l.trim();
                if (trimmed.isEmpty()) continue;
                // Detect bullet-like lines
                String displayLine = trimmed.startsWith("•") || trimmed.startsWith("-")
                        ? trimmed : trimmed;
                doc.add(new Paragraph(displayLine)
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLACK)
                        .setMarginLeft(trimmed.startsWith("•") || trimmed.startsWith("-") ? 10 : 0)
                        .setMarginBottom(2));
            }
            doc.add(new Paragraph("").setMarginBottom(6)); // section gap
        }

        doc.close();
    }

    // ── DOCX (Apache POI — structured) ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void generateDocxFile(String filePath, Map resumeMeta, List<Map> sections) throws Exception {
        org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();

        // Style name heading
        String fullName = getStr(resumeMeta, "candidateName", "Your Name");
        String jobTitle = getStr(resumeMeta, "targetJobTitle", "");

        org.apache.poi.xwpf.usermodel.XWPFParagraph namePara = doc.createParagraph();
        namePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        org.apache.poi.xwpf.usermodel.XWPFRun nameRun = namePara.createRun();
        nameRun.setBold(true);
        nameRun.setFontSize(22);
        nameRun.setText(fullName);

        if (!jobTitle.isBlank()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = titlePara.createRun();
            titleRun.setFontSize(12);
            titleRun.setText(jobTitle);
        }

        // Sections
        sections.sort((a, b) -> {
            int oa = a.get("displayOrder") instanceof Number n ? n.intValue() : 99;
            int ob = b.get("displayOrder") instanceof Number n ? n.intValue() : 99;
            return Integer.compare(oa, ob);
        });

        for (Map section : sections) {
            Boolean visible = (Boolean) section.getOrDefault("visible", true);
            if (Boolean.FALSE.equals(visible)) continue;
            String title   = getStr(section, "title", getStr(section, "sectionType", "Section"));
            String content = getStr(section, "content", "");
            if (content.isBlank()) continue;

            org.apache.poi.xwpf.usermodel.XWPFParagraph sectionHead = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun headRun = sectionHead.createRun();
            headRun.setBold(true);
            headRun.setFontSize(11);
            headRun.setText(title.toUpperCase());
            headRun.addBreak();

            for (String l : content.split("\\n")) {
                String trimmed = l.trim();
                if (trimmed.isEmpty()) continue;
                org.apache.poi.xwpf.usermodel.XWPFParagraph contentPara = doc.createParagraph();
                org.apache.poi.xwpf.usermodel.XWPFRun contentRun = contentPara.createRun();
                contentRun.setFontSize(10);
                contentRun.setText(trimmed);
            }
        }

        try (java.io.FileOutputStream out = new java.io.FileOutputStream(filePath)) {
            doc.write(out);
        }
        doc.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ExportJob createJob(ExportRequest request, ExportJob.Format format) {
        ExportJob job = ExportJob.builder()
                .resumeId(request.getResumeId())
                .userId(request.getUserId())
                .format(format)
                .status(ExportJob.Status.QUEUED)
                .templateId(request.getTemplateId())
                .customizations(request.getCustomizations())
                .build();
        return exportRepository.save(job);
    }

    private int getFileSizeKb(String filePath) {
        try { return (int) (new File(filePath).length() / 1024); } catch (Exception e) { return 0; }
    }

    private void checkDailyLimit(String userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long count = exportRepository.countByUserIdAndRequestedAtAfter(userId, startOfDay);
        if (count >= FREE_DAILY_PDF_LIMIT) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily PDF export limit reached (max " + FREE_DAILY_PDF_LIMIT + ")");
        }
    }

    private ExportJob findOrThrow(String jobId) {
        return exportRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export job not found: " + jobId));
    }

    private String getStr(Map map, String key, String fallback) {
        Object v = map.get(key);
        return (v instanceof String s && !s.isBlank()) ? s : fallback;
    }

    private ExportJobResponse toResponse(ExportJob j) {
        return ExportJobResponse.builder()
                .jobId(j.getJobId())
                .resumeId(j.getResumeId())
                .userId(j.getUserId())
                .format(j.getFormat().name())
                .status(j.getStatus().name())
                .fileUrl(j.getFileUrl())
                .fileSizeKb(j.getFileSizeKb())
                .requestedAt(j.getRequestedAt())
                .completedAt(j.getCompletedAt())
                .expiresAt(j.getExpiresAt())
                .build();
    }
}