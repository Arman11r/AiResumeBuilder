package com.resumeai.section.service;

import com.resumeai.section.dto.CreateSectionRequest;
import com.resumeai.section.dto.SectionOrderDTO;
import com.resumeai.section.dto.SectionResponse;
import com.resumeai.section.dto.UpdateSectionRequest;
import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.repository.ResumeSectionRepository;
import com.resumeai.section.service.impl.SectionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit tests verifying the core logic of this service.
@ExtendWith(MockitoExtension.class)
@DisplayName("SectionServiceImpl Tests")
class SectionServiceImplTest {

    @Mock private ResumeSectionRepository sectionRepository;

    @InjectMocks
    private SectionServiceImpl sectionService;

    // ── Shared helpers ────────────────────────────────────────────────────────

    private ResumeSection buildSection(String id, String resumeId, int order) {
        return ResumeSection.builder()
                .sectionId(id)
                .resumeId(resumeId)
                .sectionType(ResumeSection.SectionType.EXPERIENCE)
                .title("Experience")
                .content("Worked at ACME Corp.")
                .displayOrder(order)
                .isVisible(true)
                .aiGenerated(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createSection()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSection()")
    class CreateSection {

        @Test
        @DisplayName("should save and return a new section")
        void createSection_validRequest_returnsResponse() {
            // 1. Set up the test conditions
            CreateSectionRequest request = new CreateSectionRequest();
            request.setResumeId("resume-001");
            request.setSectionType("EXPERIENCE");
            request.setTitle("Experience");
            request.setContent("Worked at ACME Corp.");
            request.setDisplayOrder(1);
            request.setVisible(true);

            ResumeSection saved = buildSection("section-001", "resume-001", 1);
            when(sectionRepository.save(any(ResumeSection.class))).thenReturn(saved);

            // 2. Run the method under test
            SectionResponse response = sectionService.createSection(request);

            // 3. Verify the outcome
            assertThat(response.getSectionId()).isEqualTo("section-001");
            assertThat(response.getSectionType()).isEqualTo("EXPERIENCE");
            verify(sectionRepository).save(any(ResumeSection.class));
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for invalid section type")
        void createSection_invalidSectionType_throwsBadRequest() {
            // 1. Set up the test conditions
            CreateSectionRequest request = new CreateSectionRequest();
            request.setResumeId("resume-001");
            request.setSectionType("UNKNOWN_TYPE");
            request.setTitle("Unknown");

            // Run and verify the expected outcome
            assertThatThrownBy(() -> sectionService.createSection(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid section type");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSectionById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSectionById()")
    class GetSectionById {

        @Test
        @DisplayName("should return section when it exists")
        void getSectionById_existingSection_returnsResponse() {
            // 1. Set up the test conditions
            ResumeSection section = buildSection("section-001", "resume-001", 1);
            when(sectionRepository.findById("section-001")).thenReturn(Optional.of(section));

            // 2. Run the method under test
            SectionResponse response = sectionService.getSectionById("section-001");

            // 3. Verify the outcome
            assertThat(response.getSectionId()).isEqualTo("section-001");
        }

        @Test
        @DisplayName("should throw NOT_FOUND when section does not exist")
        void getSectionById_nonExisting_throwsNotFound() {
            // 1. Set up the test conditions
            when(sectionRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> sectionService.getSectionById("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Section not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSectionsByResumeId()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSectionsByResumeId()")
    class GetSectionsByResumeId {

        @Test
        @DisplayName("should return all sections ordered by displayOrder")
        void getSectionsByResumeId_returnsOrderedList() {
            // 1. Set up the test conditions
            ResumeSection s1 = buildSection("s-001", "resume-001", 1);
            ResumeSection s2 = buildSection("s-002", "resume-001", 2);
            when(sectionRepository.findByResumeIdOrderByDisplayOrderAsc("resume-001"))
                    .thenReturn(List.of(s1, s2));

            // 2. Run the method under test
            List<SectionResponse> responses = sectionService.getSectionsByResumeId("resume-001");

            // 3. Verify the outcome
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getDisplayOrder()).isEqualTo(1);
            assertThat(responses.get(1).getDisplayOrder()).isEqualTo(2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateSection()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateSection()")
    class UpdateSection {

        @Test
        @DisplayName("should update title and content when provided")
        void updateSection_validFields_updatesSection() {
            // 1. Set up the test conditions
            ResumeSection section = buildSection("section-001", "resume-001", 1);
            when(sectionRepository.findById("section-001")).thenReturn(Optional.of(section));
            when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateSectionRequest request = new UpdateSectionRequest();
            request.setTitle("Updated Title");
            request.setContent("Updated content here.");

            // 2. Run the method under test
            SectionResponse response = sectionService.updateSection("section-001", request);

            // 3. Verify the outcome
            assertThat(response.getTitle()).isEqualTo("Updated Title");
            assertThat(response.getContent()).isEqualTo("Updated content here.");
            verify(sectionRepository).save(any(ResumeSection.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reorderSections()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reorderSections()")
    class ReorderSections {

        @Test
        @DisplayName("should apply new display orders to all sections")
        void reorderSections_validList_appliesOrders() {
            // 1. Set up the test conditions
            ResumeSection s1 = buildSection("s-001", "resume-001", 2);
            ResumeSection s2 = buildSection("s-002", "resume-001", 1);
            when(sectionRepository.findAllById(any())).thenReturn(List.of(s1, s2));

            List<SectionOrderDTO> newOrder = List.of(
                    new SectionOrderDTO("s-001", 1),
                    new SectionOrderDTO("s-002", 2)
            );

            // 2. Run the method under test
            sectionService.reorderSections(newOrder);

            // 3. Verify the outcome
            assertThat(s1.getDisplayOrder()).isEqualTo(1);
            assertThat(s2.getDisplayOrder()).isEqualTo(2);
            verify(sectionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when a section ID is missing")
        void reorderSections_missingSectionId_throwsBadRequest() {
            // 1. Set up the test conditions
            // The repo returns only 1 section but 2 IDs were requested
            ResumeSection s1 = buildSection("s-001", "resume-001", 1);
            when(sectionRepository.findAllById(any())).thenReturn(List.of(s1));

            List<SectionOrderDTO> newOrder = List.of(
                    new SectionOrderDTO("s-001", 1),
                    new SectionOrderDTO("ghost-id", 2) // does not exist
            );

            // Run and verify the expected outcome
            assertThatThrownBy(() -> sectionService.reorderSections(newOrder))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("One or more section IDs not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toggleVisibility()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleVisibility()")
    class ToggleVisibility {

        @Test
        @DisplayName("should flip visible from true to false")
        void toggleVisibility_visibleSection_becomesHidden() {
            // 1. Set up the test conditions
            ResumeSection section = buildSection("s-001", "resume-001", 1);
            section.setVisible(true);
            when(sectionRepository.findById("s-001")).thenReturn(Optional.of(section));
            when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            SectionResponse response = sectionService.toggleVisibility("s-001");

            // 3. Verify the outcome
            assertThat(response.isVisible()).isFalse();
        }

        @Test
        @DisplayName("should flip visible from false to true")
        void toggleVisibility_hiddenSection_becomesVisible() {
            // 1. Set up the test conditions
            ResumeSection section = buildSection("s-001", "resume-001", 1);
            section.setVisible(false);
            when(sectionRepository.findById("s-001")).thenReturn(Optional.of(section));
            when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            SectionResponse response = sectionService.toggleVisibility("s-001");

            // 3. Verify the outcome
            assertThat(response.isVisible()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteSection()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteSection()")
    class DeleteSection {

        @Test
        @DisplayName("should delete section when it exists")
        void deleteSection_existingSection_deletesFromRepo() {
            // 1. Set up the test conditions
            ResumeSection section = buildSection("s-001", "resume-001", 1);
            when(sectionRepository.findById("s-001")).thenReturn(Optional.of(section));

            // 2. Run the method under test
            sectionService.deleteSection("s-001");

            // 3. Verify the outcome
            verify(sectionRepository).delete(section);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when section does not exist")
        void deleteSection_nonExisting_throwsNotFound() {
            // 1. Set up the test conditions
            when(sectionRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> sectionService.deleteSection("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Section not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteSectionsByResumeId()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteSectionsByResumeId()")
    class DeleteSectionsByResumeId {

        @Test
        @DisplayName("should call deleteByResumeId on the repository")
        void deleteSectionsByResumeId_callsRepository() {
            // 1. Set up the test conditions

            // 2. Run the method under test
            sectionService.deleteSectionsByResumeId("resume-001");

            // 3. Verify the outcome
            verify(sectionRepository).deleteByResumeId("resume-001");
        }
    }
}
