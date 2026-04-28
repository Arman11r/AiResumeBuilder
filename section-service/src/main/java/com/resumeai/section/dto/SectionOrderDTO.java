package com.resumeai.section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SectionOrderDTO {

    @NotBlank
    private String sectionId;

    @NotNull
    private Integer displayOrder;
}