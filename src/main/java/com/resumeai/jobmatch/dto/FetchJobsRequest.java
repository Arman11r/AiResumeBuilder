package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FetchJobsRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String resumeId;
    @NotBlank
    private String jobTitle;
    private String location;
    private Integer limit;
}