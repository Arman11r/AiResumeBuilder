package com.resumeai.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String provider;
    private boolean active;
    private String subscriptionPlan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}