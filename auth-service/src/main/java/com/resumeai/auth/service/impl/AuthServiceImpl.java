package com.resumeai.auth.service.impl;

import com.resumeai.auth.dto.*;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.AuthService;
import com.resumeai.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.USER)
                .provider(User.Provider.LOCAL)
                .isActive(true)
                .subscriptionPlan(User.SubscriptionPlan.FREE)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().name());
        return new AuthResponse(token, user.getUserId(), user.getEmail(),
                user.getRole().name(), user.getSubscriptionPlan().name());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account deactivated");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().name());
        return new AuthResponse(token, user.getUserId(), user.getEmail(),
                user.getRole().name(), user.getSubscriptionPlan().name());
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(String token) {
        // Mocking verification for demonstration. In production use GoogleIdTokenVerifier.
        String googleEmail = "google.user@example.com"; 
        String googleName = "Google User";
        
        // If we want to simulate decoding the JWT without verification:
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length > 1) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
                com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                if (jsonNode.has("email")) googleEmail = jsonNode.get("email").asText();
                if (jsonNode.has("name")) googleName = jsonNode.get("name").asText();
            }
        } catch (Exception e) {
            // Ignore parse errors, fallback to dummy
        }

        final String finalEmail = googleEmail;
        final String finalName = googleName;

        User user = userRepository.findByEmail(finalEmail)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .fullName(finalName)
                            .email(finalEmail)
                            .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                            .role(User.Role.USER)
                            .provider(User.Provider.GOOGLE)
                            .isActive(true)
                            .subscriptionPlan(User.SubscriptionPlan.FREE)
                            .build();
                    return userRepository.save(newUser);
                });

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account deactivated");
        }

        String jwtToken = jwtUtil.generateToken(user.getUserId(), user.getRole().name());
        return new AuthResponse(jwtToken, user.getUserId(), user.getEmail(),
                user.getRole().name(), user.getSubscriptionPlan().name());
    }

    @Override
    public void logout(String token) {
        // Stateless JWT — client discards token.
        // TODO: add token blacklist via Redis when needed.
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Override
    public AuthResponse refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        String userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account deactivated");
        }
        String newToken = jwtUtil.generateToken(user.getUserId(), user.getRole().name());
        return new AuthResponse(newToken, user.getUserId(), user.getEmail(),
                user.getRole().name(), user.getSubscriptionPlan().name());
    }

    @Override
    public UserResponseDTO getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateSubscription(String userId, String plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        try {
            user.setSubscriptionPlan(User.SubscriptionPlan.valueOf(plan.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid subscription plan: " + plan);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateAccount(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }


    private UserResponseDTO toUserResponse(User user) {
        return new UserResponseDTO(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getProvider().name(),
                user.isActive(),
                user.getSubscriptionPlan().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}