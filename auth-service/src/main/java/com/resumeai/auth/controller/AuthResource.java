package com.resumeai.auth.controller;

import com.resumeai.auth.dto.*;
import com.resumeai.auth.service.AuthService;
import com.resumeai.auth.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google token is required");
        }
        return ResponseEntity.ok(authService.googleLogin(token));
    }

    // ── Protected endpoints (JWT required) ───────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        return ResponseEntity.ok(authService.refreshToken(token));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        String userId = extractUserId(authHeader);
        authService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/subscription")
    public ResponseEntity<Void> updateSubscription(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        String userId = extractUserId(authHeader);
        String plan = body.get("plan");
        if (plan == null || plan.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "plan field is required");
        }
        authService.updateSubscription(userId, plan);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/deactivate")
    public ResponseEntity<Void> deactivateAccount(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        authService.deactivateAccount(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Inter-service endpoint (called by resume-service, ai-service, etc.) ──

    /**
     * Called by other microservices to check a user's subscription plan.
     * GET /auth/users/{userId}/subscription
     * Returns: { "subscriptionPlan": "FREE" | "PREMIUM" }
     */
    @GetMapping("/users/{userId}/subscription")
    public ResponseEntity<Map<String, String>> getSubscription(@PathVariable String userId) {
        UserResponseDTO user = authService.getUserById(userId);
        return ResponseEntity.ok(Map.of("subscriptionPlan", user.getSubscriptionPlan()));
    }

    /**
     * Called by other microservices to get full user info.
     * GET /auth/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return authHeader.substring(7);
    }

    private String extractUserId(String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return jwtUtil.extractUserId(token);
    }
}