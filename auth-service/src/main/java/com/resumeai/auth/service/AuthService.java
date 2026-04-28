package com.resumeai.auth.service;

import com.resumeai.auth.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse googleLogin(String token);
    void logout(String token);
    boolean validateToken(String token);
    AuthResponse refreshToken(String token);
    UserResponseDTO getUserById(String userId);
    UserResponseDTO updateProfile(String userId, UpdateProfileRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    void updateSubscription(String userId, String plan);
    void deactivateAccount(String userId);
}