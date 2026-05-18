package com.resumeai.auth.service;

import com.resumeai.auth.dto.*;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.impl.AuthServiceImpl;
import com.resumeai.auth.util.JwtUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit tests verifying the core logic of this service.
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private User buildActiveUser() {
        return User.builder()
                .userId("user-uuid-001")
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("$2a$hashed")
                .role(User.Role.USER)
                .provider(User.Provider.LOCAL)
                .isActive(true)
                .subscriptionPlan(User.SubscriptionPlan.FREE)
                .build();
    }

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("John Doe");
        req.setEmail("john@example.com");
        req.setPassword("secret123");
        req.setPhone("+91-9000000000");
        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register a new user and return a JWT token")
        void register_success() {
            // 1. Set up the test conditions
            RegisterRequest request = buildRegisterRequest();
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
            User saved = buildActiveUser();
            when(userRepository.save(any(User.class))).thenReturn(saved);
            when(jwtUtil.generateToken(any(), eq("USER"))).thenReturn("jwt-token");

            // 2. Run the method under test
            AuthResponse response = authService.register(request);

            // 3. Verify the outcome
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getRole()).isEqualTo("USER");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw CONFLICT when email is already registered")
        void register_emailAlreadyExists_throwsConflict() {
            // 1. Set up the test conditions
            RegisterRequest request = buildRegisterRequest();
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // login()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should return JWT token on valid credentials")
        void login_validCredentials_returnsToken() {
            // 1. Set up the test conditions
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("secret123");

            User user = buildActiveUser();
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret123", "$2a$hashed")).thenReturn(true);
            when(jwtUtil.generateToken("user-uuid-001", "USER")).thenReturn("jwt-token");

            // 2. Run the method under test
            AuthResponse response = authService.login(request);

            // 3. Verify the outcome
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getUserId()).isEqualTo("user-uuid-001");
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when email is not found")
        void login_unknownEmail_throwsUnauthorized() {
            // 1. Set up the test conditions
            LoginRequest request = new LoginRequest();
            request.setEmail("ghost@example.com");
            request.setPassword("any");
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when password does not match")
        void login_wrongPassword_throwsUnauthorized() {
            // 1. Set up the test conditions
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("wrong");

            User user = buildActiveUser();
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when account is deactivated")
        void login_deactivatedAccount_throwsUnauthorized() {
            // 1. Set up the test conditions
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("secret123");

            User user = buildActiveUser();
            user.setActive(false);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Account deactivated");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("should return true for a valid token")
        void validateToken_validToken_returnsTrue() {
            // 1. Set up the test conditions
            when(jwtUtil.validateToken("valid-jwt")).thenReturn(true);

            // 2. Run the method under test
            boolean result = authService.validateToken("valid-jwt");

            // 3. Verify the outcome
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for an invalid token")
        void validateToken_invalidToken_returnsFalse() {
            // 1. Set up the test conditions
            when(tokenBlacklistService.isBlacklisted("bad-jwt")).thenReturn(false);
            when(jwtUtil.validateToken("bad-jwt")).thenReturn(false);

            // 2. Run the method under test
            boolean result = authService.validateToken("bad-jwt");

            // 3. Verify the outcome
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for a blacklisted token")
        void validateToken_blacklistedToken_returnsFalse() {
            // 1. Set up the test conditions
            when(tokenBlacklistService.isBlacklisted("blacklisted-jwt")).thenReturn(true);

            // 2. Run the method under test
            boolean result = authService.validateToken("blacklisted-jwt");

            // 3. Verify the outcome
            assertThat(result).isFalse();
            verify(jwtUtil, never()).validateToken(anyString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // refreshToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("should issue a new token when current token is valid")
        void refreshToken_validToken_returnsNewToken() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(jwtUtil.validateToken("old-jwt")).thenReturn(true);
            when(jwtUtil.extractUserId("old-jwt")).thenReturn("user-uuid-001");
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken("user-uuid-001", "USER")).thenReturn("new-jwt");

            // 2. Run the method under test
            AuthResponse response = authService.refreshToken("old-jwt");

            // 3. Verify the outcome
            assertThat(response.getToken()).isEqualTo("new-jwt");
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when token is expired/invalid")
        void refreshToken_invalidToken_throwsUnauthorized() {
            // 1. Set up the test conditions
            when(jwtUtil.validateToken("expired-jwt")).thenReturn(false);

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.refreshToken("expired-jwt"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid or expired token");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("should return UserResponseDTO when user exists")
        void getUserById_existingUser_returnsDTO() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));

            // 2. Run the method under test
            UserResponseDTO dto = authService.getUserById("user-uuid-001");

            // 3. Verify the outcome
            assertThat(dto.getUserId()).isEqualTo("user-uuid-001");
            assertThat(dto.getEmail()).isEqualTo("john@example.com");
            assertThat(dto.getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void getUserById_nonExistingUser_throwsNotFound() {
            // 1. Set up the test conditions
            when(userRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.getUserById("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateProfile()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("should update full name and phone when both are provided")
        void updateProfile_withAllFields_updatesUser() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("Jane Doe");
            request.setPhone("+91-9999999999");

            // 2. Run the method under test
            UserResponseDTO dto = authService.updateProfile("user-uuid-001", request);

            // 3. Verify the outcome
            assertThat(dto.getFullName()).isEqualTo("Jane Doe");
            assertThat(dto.getPhone()).isEqualTo("+91-9999999999");
            verify(userRepository).save(any(User.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // changePassword()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("should change password when current password matches")
        void changePassword_correctCurrentPassword_savesNewHash() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret123", "$2a$hashed")).thenReturn(true);
            when(passwordEncoder.encode("newpass456")).thenReturn("$2a$newhash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("secret123");
            request.setNewPassword("newpass456");

            // 2. Run the method under test
            authService.changePassword("user-uuid-001", request);

            // 3. Verify the outcome
            assertThat(user.getPasswordHash()).isEqualTo("$2a$newhash");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when current password is incorrect")
        void changePassword_wrongCurrentPassword_throwsBadRequest() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("wrong");
            request.setNewPassword("newpass456");

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.changePassword("user-uuid-001", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Current password is incorrect");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateSubscription()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateSubscription()")
    class UpdateSubscription {

        @Test
        @DisplayName("should update subscription plan to PREMIUM")
        void updateSubscription_validPlan_savesUser() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            authService.updateSubscription("user-uuid-001", "PREMIUM");

            // 3. Verify the outcome
            assertThat(user.getSubscriptionPlan()).isEqualTo(User.SubscriptionPlan.PREMIUM);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for invalid subscription plan")
        void updateSubscription_invalidPlan_throwsBadRequest() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.updateSubscription("user-uuid-001", "GOLD"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid subscription plan");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deactivateAccount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivateAccount()")
    class DeactivateAccount {

        @Test
        @DisplayName("should set isActive to false and save")
        void deactivateAccount_activeUser_setsInactive() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            authService.deactivateAccount("user-uuid-001");

            // 3. Verify the outcome
            assertThat(user.isActive()).isFalse();
            verify(userRepository).save(user);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getPlatformStats()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPlatformStats()")
    class GetPlatformStats {

        @Test
        @DisplayName("should compute correct totals from user list")
        void getPlatformStats_mixedUsers_returnsCorrectCounts() {
            // 1. Set up the test conditions
            User activeUser = buildActiveUser();
            User premiumUser = User.builder()
                    .userId("user-uuid-002").fullName("Jane").email("jane@example.com")
                    .role(User.Role.USER).provider(User.Provider.LOCAL)
                    .isActive(true).subscriptionPlan(User.SubscriptionPlan.PREMIUM).build();
            User suspendedUser = User.builder()
                    .userId("user-uuid-003").fullName("Bob").email("bob@example.com")
                    .role(User.Role.USER).provider(User.Provider.LOCAL)
                    .isActive(false).subscriptionPlan(User.SubscriptionPlan.FREE).build();

            when(userRepository.findAll()).thenReturn(List.of(activeUser, premiumUser, suspendedUser));

            // 2. Run the method under test
            Map<String, Object> stats = authService.getPlatformStats();

            // 3. Verify the outcome
            assertThat(stats)
                    .containsEntry("totalUsers", 3L)
                    .containsEntry("activeUsers", 2L)
                    .containsEntry("premiumUsers", 1L)
                    .containsEntry("suspendedUsers", 1L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // adminSuspendUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminSuspendUser()")
    class AdminSuspendUser {

        @Test
        @DisplayName("should suspend (deactivate) a user when suspend=true")
        void adminSuspendUser_suspend_setsInactive() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            authService.adminSuspendUser("user-uuid-001", true);

            // 3. Verify the outcome
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("should unsuspend (activate) a user when suspend=false")
        void adminSuspendUser_unsuspend_setsActive() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            user.setActive(false);
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // 2. Run the method under test
            authService.adminSuspendUser("user-uuid-001", false);

            // 3. Verify the outcome
            assertThat(user.isActive()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // adminDeleteUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminDeleteUser()")
    class AdminDeleteUser {

        @Test
        @DisplayName("should delete user when found")
        void adminDeleteUser_existingUser_deletesFromRepo() {
            // 1. Set up the test conditions
            User user = buildActiveUser();
            when(userRepository.findById("user-uuid-001")).thenReturn(Optional.of(user));

            // 2. Run the method under test
            authService.adminDeleteUser("user-uuid-001");

            // 3. Verify the outcome
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void adminDeleteUser_nonExistingUser_throwsNotFound() {
            // 1. Set up the test conditions
            when(userRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Run and verify the expected outcome
            assertThatThrownBy(() -> authService.adminDeleteUser("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }
    }
}
