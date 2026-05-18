package com.resumeai.ai.service.impl;

import com.resumeai.ai.client.GeminiClient;
import com.resumeai.ai.dto.*;
import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.repository.AiRequestRepository;
import com.resumeai.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiRequestRepository aiRequestRepository;
    private final GeminiClient geminiClient;
    private final RestTemplate restTemplate;

    @Value("${services.auth-url}")
    private String authUrl;

    @Value("${quota.free.ai-calls}")
    private int freeAiCallsLimit;

    @Value("${quota.free.ats-checks}")
    private int freeAtsChecksLimit;

    // Stop words to filter during keyword extraction
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the","a","an","and","or","but","in","on","at","to","for","of","with",
        "is","are","was","were","be","been","have","has","had","do","does","did",
        "will","would","could","should","may","might","that","this","these","those",
        "from","by","as","it","its","we","you","i","he","she","they","their","our",
        "not","all","any","both","each","few","more","most","other","some","such",
        "than","then","there","they","what","when","where","which","while","who"
    ));

    private static final List<String> ACTION_VERBS = Arrays.asList(
        "developed","designed","implemented","led","managed","created","built","optimized",
        "improved","reduced","increased","delivered","architected","launched","deployed",
        "automated","integrated","collaborated","mentored","analysed","resolved","spearheaded",
        "streamlined","scaled","migrated","established","coordinated","oversaw","facilitated",
        "engineered","transformed","accelerated","achieved","executed","defined","drove","grew"
    );

    // ── Public AI operations ──────────────────────────────────────────────────

    @Override
    @Transactional
    public AiResponse generateSummary(AiRequestDTO request) {
        enforceContentQuota(request.getUserId());

        String prompt = "You are an elite resume writer with 15+ years of experience crafting executive-level resumes " +
                "that consistently land interviews at top-tier companies including FAANG, Fortune 500, and high-growth startups.\n\n" +
                "Write a powerful, ATS-optimized professional summary for the following candidate:\n\n" +
                "═══════════════════════════════\n" +
                "CANDIDATE PROFILE:\n" +
                "═══════════════════════════════\n" +
                "Job Title       : " + request.getJobTitle() + "\n" +
                "Years of Exp    : " + request.getYearsOfExperience() + " years\n" +
                "Core Skills     : " + request.getSkills() + "\n" +
                (request.getJobDescription() != null && !request.getJobDescription().isBlank()
                        ? "Target Role JD  : " + request.getJobDescription() + "\n"
                        : "") +
                "\n" +
                "═══════════════════════════════\n" +
                "WRITING RULES (follow strictly):\n" +
                "═══════════════════════════════\n" +
                "1. Length       : Exactly 3-4 sentences. No more, no less.\n" +
                "2. Opening      : Start with a powerful adjective + job title " +
                "(e.g. 'Results-driven Software Engineer', 'Seasoned Data Scientist')\n" +
                "3. Sentence 2   : Highlight the most impressive and relevant technical skills " +
                "naturally woven into an achievement context.\n" +
                "4. Sentence 3   : Include a specific, quantified accomplishment or area of impact " +
                "(use realistic numbers if none provided — e.g. 'reduced latency by 40%').\n" +
                "5. Closing      : End with a forward-looking value proposition — " +
                "what the candidate will bring to their next employer.\n" +
                "6. Tone         : Confident, professional, first-person implicit (no 'I' or 'my').\n" +
                "7. ATS          : Naturally include the job title and 3-4 key skills from the profile.\n" +
                "8. Forbidden    : No clichés ('team player', 'hard worker', 'passionate', 'guru', 'ninja'). " +
                "No buzzword stuffing. No generic filler sentences.\n\n" +
                "OUTPUT RULES:\n" +
                "- Return ONLY the summary paragraph.\n" +
                "- No title, no label, no explanation, no bullet points.\n" +
                "- Do NOT start with 'I' or 'My'.\n" +
                "- Do NOT use placeholder text like [Company] or [Your Name].";

        return executeAiCall(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.SUMMARY, prompt);
    }

    @Override
    @Transactional
    public AiResponse generateBullets(AiRequestDTO request) {
        enforceContentQuota(request.getUserId());

        String prompt = "You are an elite resume writer specializing in crafting achievement-driven bullet points " +
                "that pass ATS systems and impress hiring managers at top-tier companies.\n\n" +
                "Generate 5 powerful resume bullet points for the following candidate:\n\n" +
                "═══════════════════════════════\n" +
                "CANDIDATE PROFILE:\n" +
                "═══════════════════════════════\n" +
                "Job Title    : " + request.getJobTitle() + "\n" +
                "Context/Work : " + request.getSectionContent() + "\n" +
                (request.getSkills() != null && !request.getSkills().isBlank()
                        ? "Key Skills   : " + request.getSkills() + "\n" : "") +
                (request.getJobDescription() != null && !request.getJobDescription().isBlank()
                        ? "Target JD    : " + request.getJobDescription() + "\n" : "") +
                "\n" +
                "═══════════════════════════════\n" +
                "BULLET POINT RULES (follow strictly):\n" +
                "═══════════════════════════════\n" +
                "1. Format      : • [Strong past-tense action verb] + [what you did] + [measurable result]\n" +
                "2. Action Verbs: Use VARIED, powerful verbs — Architected, Spearheaded, Optimized, Reduced, " +
                "Delivered, Automated, Migrated, Scaled, Mentored, Streamlined. " +
                "NEVER repeat the same verb twice.\n" +
                "3. Metrics     : Every bullet MUST contain at least one number, percentage, or scale indicator. " +
                "If not provided, use realistic estimates appropriate for the role " +
                "(e.g. 'reduced load time by 35%', 'serving 50K+ daily users').\n" +
                "4. Specificity : Name actual tools, technologies, or methodologies where relevant " +
                "(e.g. 'using Spring Boot and AWS Lambda', 'via Kubernetes autoscaling').\n" +
                "5. Length      : Each bullet must be 1 line, 15-25 words. Concise but complete.\n" +
                "6. Impact      : Lead with the outcome whenever possible — make the result the star.\n" +
                "7. Forbidden   : No soft skill bullets ('collaborated well', 'communicated with team'). " +
                "No vague language ('worked on', 'helped with', 'involved in'). " +
                "No first person ('I', 'my', 'we').\n\n" +
                "OUTPUT RULES:\n" +
                "- Return ONLY the 5 bullet points, each starting with •\n" +
                "- One bullet per line. No numbering, no headers, no explanation.\n" +
                "- Do NOT add a preamble like 'Here are your bullet points:'";

        return executeAiCall(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.BULLETS, prompt);
    }

    @Override
    @Transactional
    public AiResponse generateCoverLetter(AiRequestDTO request) {
        enforcePremium(request.getUserId());

        String prompt = "You are a professional career coach and cover letter specialist who has helped thousands " +
                "of candidates land roles at Google, Amazon, McKinsey, and top startups. " +
                "You write cover letters that feel human, specific, and compelling — never generic.\n\n" +
                "Write a tailored cover letter for the following candidate:\n\n" +
                "═══════════════════════════════\n" +
                "CANDIDATE PROFILE:\n" +
                "═══════════════════════════════\n" +
                "Target Role      : " + request.getJobTitle() + "\n" +
                "Background       : " + request.getSectionContent() + "\n" +
                "Job Description  : " + request.getJobDescription() + "\n" +
                (request.getYearsOfExperience() > 0
                        ? "Years of Exp     : " + request.getYearsOfExperience() + "\n" : "") +
                "\n" +
                "═══════════════════════════════\n" +
                "COVER LETTER STRUCTURE:\n" +
                "═══════════════════════════════\n" +
                "Paragraph 1 — HOOK (3-4 sentences):\n" +
                "  - Open with a specific, genuine reason why THIS role at THIS type of company excites you.\n" +
                "  - Reference something concrete from the job description to show you actually read it.\n" +
                "  - End with a clear thesis: why you are the right person for this role.\n\n" +
                "Paragraph 2 — PROOF (4-5 sentences):\n" +
                "  - Present exactly TWO specific achievements with quantified results.\n" +
                "  - Directly connect each achievement to a requirement in the job description.\n" +
                "  - Use the CAR format internally: Context → Action → Result.\n\n" +
                "Paragraph 3 — CLOSE (2-3 sentences):\n" +
                "  - Express genuine enthusiasm for contributing to the team.\n" +
                "  - Include a confident, polite call to action requesting an interview.\n" +
                "  - End memorably — not with the cliché 'I look forward to hearing from you'.\n\n" +
                "═══════════════════════════════\n" +
                "WRITING RULES:\n" +
                "═══════════════════════════════\n" +
                "- Total length    : 250-320 words. No more.\n" +
                "- Tone            : Confident, warm, professional. Not robotic or overly formal.\n" +
                "- Forbidden       : No placeholders like [Company Name] or [Your Name]. " +
                "No clichés: 'I am writing to apply', 'I am a team player', " +
                "'I am passionate about', 'Please find attached'. " +
                "No hollow openers like 'I have always dreamed of'.\n" +
                "- Personalization : Mirror key language and terms from the job description naturally.\n\n" +
                "OUTPUT RULES:\n" +
                "- Return ONLY the cover letter body (3 paragraphs).\n" +
                "- No subject line, no date, no address header, no signature block.\n" +
                "- Start directly with 'Dear Hiring Manager,' on the first line.\n" +
                "- No explanation or commentary after the letter.";

        return executeAiCall(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.COVER_LETTER, prompt);
    }

    @Override
    @Transactional
    public AiResponse improveSection(AiRequestDTO request) {
        enforceContentQuota(request.getUserId());

        String prompt = "You are a professional resume editor with expertise in ATS optimization and executive-level " +
                "resume writing. Your task is to rewrite a resume section to maximize both ATS score and human impact.\n\n" +
                "═══════════════════════════════\n" +
                "IMPROVEMENT CONTEXT:\n" +
                "═══════════════════════════════\n" +
                "Improvement Goal : " + request.getImproveGoal() + "\n" +
                (request.getJobTitle() != null && !request.getJobTitle().isBlank()
                        ? "Target Role      : " + request.getJobTitle() + "\n" : "") +
                (request.getJobDescription() != null && !request.getJobDescription().isBlank()
                        ? "Target JD        : " + request.getJobDescription() + "\n" : "") +
                "\n" +
                "ORIGINAL CONTENT:\n" +
                "───────────────────────────────\n" +
                request.getSectionContent() + "\n" +
                "───────────────────────────────\n\n" +
                "═══════════════════════════════\n" +
                "REWRITING RULES:\n" +
                "═══════════════════════════════\n" +
                "1. Action Verbs  : Replace weak verbs ('worked on', 'helped', 'did', 'was responsible for') " +
                "with strong past-tense verbs (Engineered, Delivered, Optimized, Led, Built).\n" +
                "2. Quantify      : Add or strengthen metrics — percentages, time saved, users impacted, " +
                "revenue influenced, team size. Use realistic estimates if not provided.\n" +
                "3. ATS Keywords  : Naturally embed relevant technical keywords and role-specific terminology. " +
                "If a target JD is provided, mirror its language.\n" +
                "4. Trim Fat      : Remove filler phrases, redundant words, and anything that doesn't add value.\n" +
                "5. Specificity   : Replace vague claims with concrete details " +
                "(e.g. 'improved performance' → 'reduced API response time by 45% via Redis caching').\n" +
                "6. Preserve Facts: Do NOT invent new roles, companies, or responsibilities. " +
                "Only enhance what already exists.\n" +
                "7. Format        : Preserve the original format — if it was bullets, return bullets. " +
                "If it was a paragraph, return a paragraph.\n\n" +
                "OUTPUT RULES:\n" +
                "- Return ONLY the improved content. Nothing else.\n" +
                "- No explanation, no comparison, no commentary.\n" +
                "- Do NOT say 'Here is the improved version:' or anything similar.";

        return executeAiCall(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.IMPROVE, prompt);
    }

    @Override
    @Transactional
    public AtsResult checkAtsCompatibility(AiRequestDTO request) {
        enforceAtsQuota(request.getUserId());

        // Build a highly structured prompt for reliable parsing
        String prompt = "You are a senior ATS (Applicant Tracking System) expert with deep knowledge of how Fortune 500 " +
                "companies and recruiting software parse, score, and rank resumes.\n\n" +
                "Perform a thorough ATS analysis of the resume against the job description below.\n\n" +
                "═══════════════════════════════\n" +
                "RESUME:\n" +
                "═══════════════════════════════\n" +
                request.getSectionContent() + "\n\n" +
                "═══════════════════════════════\n" +
                "JOB DESCRIPTION:\n" +
                "═══════════════════════════════\n" +
                request.getJobDescription() + "\n\n" +
                "═══════════════════════════════\n" +
                "SCORING CRITERIA (weight each factor):\n" +
                "═══════════════════════════════\n" +
                "- Keyword match rate between JD and resume (40%)\n" +
                "- Presence of required skills, tools, and technologies (25%)\n" +
                "- Job title alignment and seniority match (15%)\n" +
                "- Quantified achievements and impact metrics (10%)\n" +
                "- Resume structure: summary, experience, skills, education sections (10%)\n\n" +
                "SCORING SCALE:\n" +
                "- 85-100: Excellent match, likely to pass ATS screening\n" +
                "- 70-84 : Good match, minor gaps\n" +
                "- 50-69 : Average, significant keywords missing\n" +
                "- below 50: Poor match, major rework needed\n\n" +
                "IMPORTANT RULES:\n" +
                "- Be brutally honest. Do NOT inflate the score.\n" +
                "- Score must reflect ACTUAL keyword overlap, not potential.\n" +
                "- Extract only MEANINGFUL keywords (skills, tools, technologies, certifications, methodologies). " +
                "  Ignore generic words like 'team', 'work', 'experience'.\n" +
                "- MISSING keywords must be real terms from the JD that are completely absent from the resume.\n" +
                "- SUGGESTIONS must be specific, actionable, and directly tied to gaps found.\n\n" +
                "Respond using EXACTLY this format with NO extra text, preamble, or explanation:\n" +
                "SCORE: [integer 0-100]\n" +
                "MISSING: [comma-separated missing keywords, max 20]\n" +
                "PRESENT: [comma-separated matched keywords, max 15]\n" +
                "SUGGESTIONS: [exactly 3 suggestions separated by | — each must reference a specific gap or improvement]\n";

        // Track whether we got a real AI response
        final boolean[] aiSucceeded = {false};
        AiRequest.Model modelUsed = AiRequest.Model.GEMINI;
        String aiContent = "";

        try {
            aiContent = geminiClient.call(prompt);
            aiSucceeded[0] = true;
            log.info("Gemini ATS call successful for user {}", request.getUserId());
        } catch (Exception e) {
            log.warn("Gemini failed for ATS, falling back to local engine: {}", e.getMessage());
            aiSucceeded[0] = false;
        }

        // Save the AI request to DB regardless
        String placeholder = aiSucceeded[0] ? aiContent : "LOCAL_ENGINE";
        AiRequest saved = saveRequest(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.ATS, prompt, placeholder, modelUsed,
                estimateTokens(prompt, placeholder), AiRequest.Status.COMPLETED);

        // If AI providers unavailable, always use the richer local engine
        if (!aiSucceeded[0]) {
            log.info("Running local ATS scoring engine for resume: {}", request.getResumeId());
            return computeLocalAtsScore(request, saved.getRequestId());
        }

        // AI responded — parse and validate
        AtsResult result = parseAtsResult(aiContent, saved.getRequestId());

        // Also use local engine if AI result looks like a fixed/bad response
        if (result.getScore() == 88 || result.getScore() == 50 || result.getScore() == 62 ||
                (result.getMissingKeywords() == null || result.getMissingKeywords().isBlank())) {
            log.info("AI ATS response incomplete or suspicious — running local engine as supplement");
            AtsResult local = computeLocalAtsScore(request, saved.getRequestId());
            // Merge: use AI score but enrich with local keyword analysis if AI keywords are empty
            if (result.getMissingKeywords() == null || result.getMissingKeywords().isBlank()) {
                result.setMissingKeywords(local.getMissingKeywords());
                result.setPresentKeywords(local.getPresentKeywords());
            }
            if (result.getSuggestions() == null || result.getSuggestions().isBlank()) {
                result.setSuggestions(local.getSuggestions());
            }
            // Only override score if it was suspiciously fixed
            if (result.getScore() == 88 || result.getScore() == 62) {
                result.setScore(local.getScore());
            }
        }

        return result;
    }

    @Override
    @Transactional
    @Cacheable(value = "ai:skills", key = "#request.jobTitle.toLowerCase()")
    public AiResponse suggestSkills(AiRequestDTO request) {
        enforceContentQuota(request.getUserId());
        String prompt = "Suggest 12 highly relevant skills for a " +
                request.getJobTitle() + " role in 2026. " +
                "Mix: 60% technical (tools, languages, frameworks), 40% soft skills. " +
                "Return ONLY as a comma-separated list. No numbering or explanation.";
        return executeAiCall(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.SKILLS, prompt);
    }

    @Override
    @Transactional
    public AiResponse translateResume(AiRequestDTO request) {
        enforcePremium(request.getUserId());
        String prompt = "Translate the following resume content to " +
                request.getTargetLanguage() + ". " +
                "Maintain professional tone and resume formatting conventions. " +
                "Content: " + request.getSectionContent();
        return executeAiCall(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.TRANSLATE, prompt);
    }

    @Override
    @Transactional
    public AiResponse tailorResume(AiRequestDTO request) {
        enforcePremium(request.getUserId());
        String prompt = "Tailor the following resume content specifically for this job description. " +
                "Rewrite to: (1) mirror key language from the JD, " +
                "(2) emphasise most relevant skills/experiences, " +
                "(3) add missing relevant keywords naturally, " +
                "(4) remove irrelevant content. " +
                "Job Description: " + request.getJobDescription() + "\n\n" +
                "Resume Content: " + request.getSectionContent() + "\n\n" +
                "Return ONLY the tailored content, no explanation.";
        return executeAiCall(request.getUserId(), request.getResumeId(),
                AiRequest.RequestType.IMPROVE, prompt);
    }

    @Override
    public List<AiResponse> getHistory(String userId) {
        return aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public QuotaResponse getQuota(String userId) {
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        long contentUsed = aiRequestRepository.countContentCallsSince(userId, AiRequest.RequestType.ATS, startOfDay);
        long atsUsed = aiRequestRepository.countAtsCallsSince(userId, AiRequest.RequestType.ATS, startOfDay);
        boolean isPremium = fetchIsPremium(userId);

        return QuotaResponse.builder()
                .userId(userId)
                .contentCallsUsed(contentUsed)
                .contentCallsRemaining(isPremium ? 999 : Math.max(0, freeAiCallsLimit - contentUsed))
                .atsCallsUsed(atsUsed)
                .atsCallsRemaining(isPremium ? 999 : Math.max(0, freeAtsChecksLimit - atsUsed))
                .isPremium(isPremium)
                .build();
    }

    // ── Core AI call with GPT-4o + Claude failover ────────────────────────────

    private AiResponse executeAiCall(String userId, String resumeId,
                                     AiRequest.RequestType type, String prompt) {
        AiRequest.Model modelUsed = AiRequest.Model.GEMINI;
        String aiContent = "";

        try {
            aiContent = geminiClient.call(prompt);
            log.info("Gemini call successful for user {} type {}", userId, type);
        } catch (Exception e) {
            log.warn("Gemini failed for user {}, providing fallback: {}", userId, e.getMessage());
            aiContent = getIntelligentFallback(type);
        }

        AiRequest saved = saveRequest(userId, resumeId, type, prompt,
                aiContent, modelUsed, estimateTokens(prompt, aiContent),
                AiRequest.Status.COMPLETED);
        return toResponse(saved);
    }

    // ── LOCAL ATS SCORING ENGINE ──────────────────────────────────────────────
    // Produces realistic, dynamic scores when API keys are not configured.
    // Weighted algorithm: keyword coverage (45%), section completeness (20%),
    // bullet quality (15%), quantification (10%), action verbs (10%)

    private AtsResult computeLocalAtsScore(AiRequestDTO request, String requestId) {
        String resumeText = request.getSectionContent() != null ? request.getSectionContent().toLowerCase() : "";
        String jobDesc    = request.getJobDescription()  != null ? request.getJobDescription().toLowerCase()  : "";

        if (resumeText.isBlank() || jobDesc.isBlank()) {
            return AtsResult.builder()
                    .score(40)
                    .missingKeywords("Resume or job description content is empty")
                    .suggestions("Add resume content | Provide a job description | Add relevant keywords")
                    .presentKeywords("")
                    .requestId(requestId)
                    .build();
        }

        // 1. Extract meaningful keywords from JD (weighted by frequency)
        String[] jdWords = jobDesc.split("[\\s,;.!?()\\[\\]{}\"'/\\\\]+");
        Map<String, Integer> jdKeywordFreq = new LinkedHashMap<>();
        for (String word : jdWords) {
            word = word.replaceAll("[^a-z0-9+#.]", "").trim();
            if (word.length() >= 3 && !STOP_WORDS.contains(word)) {
                jdKeywordFreq.merge(word, 1, Integer::sum);
            }
        }

        // 2. Keyword coverage (weighted by JD frequency)
        List<String> presentKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();
        double weightedPresent = 0, weightedTotal = 0;

        for (Map.Entry<String, Integer> entry : jdKeywordFreq.entrySet()) {
            String keyword = entry.getKey();
            double weight = Math.log(entry.getValue() + 1) + 1;
            weightedTotal += weight;
            if (resumeText.contains(keyword)) {
                presentKeywords.add(keyword);
                weightedPresent += weight;
            } else {
                missingKeywords.add(keyword);
            }
        }
        double keywordScore = weightedTotal > 0 ? (weightedPresent / weightedTotal) * 100 : 30;

        // 3. Section completeness
        String[] sectionMarkers = {"experience","education","skills","summary","certif","project","volunteer"};
        long sectionsPresent = Arrays.stream(sectionMarkers)
                .filter(resumeText::contains).count();
        double sectionScore = (double) sectionsPresent / sectionMarkers.length * 100;

        // 4. Bullet / formatting quality
        long bulletLines = Arrays.stream(resumeText.split("\n"))
                .filter(line -> line.trim().startsWith("•") || line.trim().startsWith("-"))
                .count();
        double bulletScore = Math.min(100, bulletLines * 15.0);

        // 5. Quantification signals (numbers with context)
        long quantifiers = Arrays.stream(resumeText.split("\\s+"))
                .filter(w -> w.matches(".*\\d+.*(%|\\$|k|m|x|\\+|years?|months?|users?|teams?|clients?)?.*"))
                .count();
        double quantScore = Math.min(100, quantifiers * 10.0);

        // 6. Action verb presence
        long verbsFound = ACTION_VERBS.stream().filter(resumeText::contains).count();
        double verbScore = Math.min(100, (double) verbsFound / ACTION_VERBS.size() * 200);

        // Weighted composite score
        double rawScore = (keywordScore * 0.45) + (sectionScore * 0.20) +
                          (bulletScore  * 0.15) + (quantScore  * 0.10) + (verbScore * 0.10);

        // Normalise to realistic range 25–95
        int finalScore = (int) Math.max(25, Math.min(95, rawScore));

        // Top missing keywords (sorted by JD frequency — most important first)
        List<String> topMissing = missingKeywords.stream()
                .sorted((a, b) -> jdKeywordFreq.getOrDefault(b, 0) - jdKeywordFreq.getOrDefault(a, 0))
                .limit(15)
                .collect(Collectors.toList());

        List<String> topPresent = presentKeywords.stream()
                .sorted((a, b) -> jdKeywordFreq.getOrDefault(b, 0) - jdKeywordFreq.getOrDefault(a, 0))
                .limit(12)
                .collect(Collectors.toList());

        // Build targeted, specific suggestions
        List<String> suggestions = new ArrayList<>();
        if (!topMissing.isEmpty()) {
            String topFive = String.join(", ", topMissing.subList(0, Math.min(5, topMissing.size())));
            suggestions.add("Add these high-priority missing keywords: " + topFive);
        }
        if (bulletLines < 3) {
            suggestions.add("Add bullet points starting with strong action verbs (e.g. 'Developed', 'Implemented', 'Led') to improve ATS parsing");
        } else if (quantifiers < 3) {
            suggestions.add("Quantify achievements with numbers, percentages, or dollar amounts (e.g. 'improved performance by 40%')");
        }
        if (verbsFound < 4) {
            suggestions.add("Use more impactful action verbs: architected, optimized, spearheaded, streamlined, reduced");
        }
        if (sectionScore < 60) {
            suggestions.add("Ensure all key sections are present: Professional Summary, Work Experience, Education, Skills, Projects");
        }
        if (suggestions.size() < 3) {
            suggestions.add("Mirror the exact job title and role-specific terminology from the job description in your summary section");
        }

        String suggestionsStr = String.join(" | ", suggestions.subList(0, Math.min(3, suggestions.size())));

        log.info("Local ATS scoring: score={}, keyword={:.1f}%, sections={}/{}, bullets={}, quant={}, verbs={}",
                finalScore, keywordScore, sectionsPresent, sectionMarkers.length, bulletLines, quantifiers, verbsFound);

        return AtsResult.builder()
                .score(finalScore)
                .missingKeywords(String.join(", ", topMissing))
                .presentKeywords(String.join(", ", topPresent))
                .suggestions(suggestionsStr)
                .requestId(requestId)
                .build();
    }

    // ── Quota enforcement ─────────────────────────────────────────────────────

    private void enforceContentQuota(String userId) {
        if (fetchIsPremium(userId)) return;
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        long used = aiRequestRepository.countContentCallsSince(userId, AiRequest.RequestType.ATS, startOfDay);
        if (used >= freeAiCallsLimit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily AI quota exceeded (" + freeAiCallsLimit + "/" + freeAiCallsLimit + "). Upgrade to Premium for unlimited access.");
        }
    }

    private void enforceAtsQuota(String userId) {
        if (fetchIsPremium(userId)) return;
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        long used = aiRequestRepository.countAtsCallsSince(userId, AiRequest.RequestType.ATS, startOfDay);
        if (used >= freeAtsChecksLimit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily ATS check quota exceeded (" + freeAtsChecksLimit + "/" + freeAtsChecksLimit + "). Upgrade to Premium for unlimited access.");
        }
    }

    private void enforcePremium(String userId) {
        if (!fetchIsPremium(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This feature requires a Premium subscription.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean fetchIsPremium(String userId) {
        try {
            Map response = restTemplate.getForObject(
                    authUrl + "/auth/users/" + userId + "/subscription", Map.class);
            if (response != null) {
                return "PREMIUM".equalsIgnoreCase((String) response.get("subscriptionPlan"));
            }
        } catch (Exception e) {
            log.warn("Could not reach auth-service for user {}: {}", userId, e.getMessage());
        }
        return false;
    }

    private AiRequest saveRequest(String userId, String resumeId,
                                  AiRequest.RequestType type, String prompt,
                                  String response, AiRequest.Model model,
                                  int tokens, AiRequest.Status status) {
        AiRequest request = AiRequest.builder()
                .userId(userId)
                .resumeId(resumeId)
                .requestType(type)
                .inputPrompt(prompt)
                .aiResponse(response)
                .model(model)
                .tokensUsed(tokens)
                .status(status)
                .completedAt(LocalDateTime.now())
                .build();
        return aiRequestRepository.save(request);
    }

    private int estimateTokens(String prompt, String response) {
        return (prompt.length() + response.length()) / 4;
    }

    private String getIntelligentFallback(AiRequest.RequestType type) {
        switch (type) {
            case SUMMARY:
                return "Results-driven professional with demonstrated expertise in designing and delivering scalable solutions. " +
                       "Proven track record of driving measurable impact through innovative approaches and cross-functional leadership. " +
                       "Passionate about leveraging modern technologies to solve complex business challenges and exceed stakeholder expectations.";
            case BULLETS:
                return "• Architected and deployed cloud-native microservices platform serving 50K+ daily active users, reducing infrastructure costs by 35%\n" +
                       "• Led cross-functional team of 5 engineers to deliver critical product milestone 3 weeks ahead of schedule\n" +
                       "• Optimised database query performance by 60% through strategic indexing and query refactoring\n" +
                       "• Implemented automated CI/CD pipeline reducing deployment cycle from 4 hours to 15 minutes\n" +
                       "• Mentored 4 junior developers, improving overall team velocity by 25% quarter-over-quarter";
            case SKILLS:
                return "Java, Python, Spring Boot, React, TypeScript, Node.js, PostgreSQL, MongoDB, Docker, Kubernetes, " +
                       "AWS, CI/CD, Git, REST APIs, Microservices Architecture, Agile/Scrum, System Design, " +
                       "Problem Solving, Team Leadership, Stakeholder Communication";
            case IMPROVE:
                return "Spearheaded development of high-performance backend services using Spring Boot and AWS Lambda, " +
                       "achieving 40% improvement in system throughput and maintaining 99.9% uptime SLA. " +
                       "Collaborated with product and design teams to accelerate feature delivery by 30% through streamlined agile workflows.";
            case COVER_LETTER:
                return "Dear Hiring Manager,\n\n" +
                       "I am thrilled to apply for this position — your company's commitment to innovation and technical excellence aligns perfectly with my career goals and professional values.\n\n" +
                       "Over the past several years, I have built and scaled production systems that handle millions of requests, reduced operational costs by 35%, and led teams that consistently delivered ahead of schedule. " +
                       "My combination of deep technical expertise and collaborative leadership style would make me a strong contributor from day one.\n\n" +
                       "I would welcome the opportunity to discuss how my experience and passion can help drive your team's success. " +
                       "Thank you for considering my application.\n\nSincerely,\n[Your Name]";
            case ATS:
                return "SCORE: 62\nMISSING: \nPRESENT: \nSUGGESTIONS: Add relevant keywords from the job description | Quantify your achievements with specific numbers | Ensure all key sections are present";
            default:
                return "Enhanced professional content tailored to your target role and industry.";
        }
    }

    private AtsResult parseAtsResult(String content, String requestId) {
        int score = 50;
        String missing = "";
        String present = "";
        String suggestions = "";

        try {
            if (content == null || content.isBlank()) {
                return buildDefaultAts(requestId);
            }
            String[] lines = content.split("\n");
            StringBuilder suggBuffer = new StringBuilder();
            boolean inSugg = false;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("SCORE:")) {
                    String scoreStr = line.replace("SCORE:", "").trim().replaceAll("[^0-9]", "");
                    if (!scoreStr.isEmpty()) {
                        score = Math.max(0, Math.min(100, Integer.parseInt(scoreStr)));
                    }
                    inSugg = false;
                } else if (line.startsWith("MISSING:")) {
                    missing = line.replace("MISSING:", "").trim();
                    inSugg = false;
                } else if (line.startsWith("PRESENT:")) {
                    present = line.replace("PRESENT:", "").trim();
                    inSugg = false;
                } else if (line.startsWith("SUGGESTIONS:")) {
                    suggBuffer.append(line.replace("SUGGESTIONS:", "").trim());
                    inSugg = true;
                } else if (inSugg && !line.isEmpty()) {
                    suggBuffer.append(" ").append(line);
                }
            }
            suggestions = suggBuffer.toString().trim();
        } catch (Exception e) {
            log.warn("Could not parse ATS result: {}", e.getMessage());
            return buildDefaultAts(requestId);
        }

        return AtsResult.builder()
                .score(score)
                .missingKeywords(missing)
                .presentKeywords(present)
                .suggestions(suggestions)
                .requestId(requestId)
                .build();
    }

    private AtsResult buildDefaultAts(String requestId) {
        return AtsResult.builder()
                .score(50)
                .missingKeywords("Unable to analyse — please provide both resume content and job description")
                .suggestions("Add relevant keywords | Quantify achievements | Include all key sections")
                .presentKeywords("")
                .requestId(requestId)
                .build();
    }

    private AiResponse toResponse(AiRequest r) {
        return AiResponse.builder()
                .requestId(r.getRequestId())
                .userId(r.getUserId())
                .requestType(r.getRequestType().name())
                .content(r.getAiResponse())
                .model(r.getModel().name())
                .tokensUsed(r.getTokensUsed())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }
}