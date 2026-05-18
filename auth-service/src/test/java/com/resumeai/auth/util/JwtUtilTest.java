package com.resumeai.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

// Unit tests verifying the core logic of this service.
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // A 256-bit (32-byte) secret required by HS256
    private static final String TEST_SECRET  = "my-super-secret-key-for-resumeai-tests-1234";
    private static final long   TEST_EXPIRY  = 3_600_000L; // 1 hour in ms

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",   TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiryMs", TEST_EXPIRY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("should return a non-null, non-blank JWT string")
        void generateToken_validInput_returnsJwtString() {
            // 1. Set up the test conditions
            String userId = "user-uuid-001";
            String role   = "USER";

            // 2. Run the method under test
            String token = jwtUtil.generateToken(userId, role);

            // 3. Verify the outcome
            assertThat(token).isNotNull().isNotBlank();
            // JWT format: three base64url segments separated by dots
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("should generate distinct tokens for different user IDs")
        void generateToken_differentUsers_returnsDistinctTokens() {
            // 1. Set up the test conditions
            String token1 = jwtUtil.generateToken("user-001", "USER");
            String token2 = jwtUtil.generateToken("user-002", "USER");

            // Run and verify the expected outcome
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should embed the correct userId as JWT subject")
        void generateToken_embedsUserId() {
            // 1. Set up the test conditions
            String userId = "user-uuid-001";

            // 2. Run the method under test
            String token   = jwtUtil.generateToken(userId, "USER");
            String subject = jwtUtil.extractUserId(token);

            // 3. Verify the outcome
            assertThat(subject).isEqualTo(userId);
        }

        @Test
        @DisplayName("should embed the correct role claim inside the token")
        void generateToken_embedsRole() {
            // 1. Set up the test conditions
            String expectedRole = "ADMIN";

            // 2. Run the method under test
            String token        = jwtUtil.generateToken("user-uuid-001", expectedRole);
            String extractedRole = jwtUtil.extractRole(token);

            // 3. Verify the outcome
            assertThat(extractedRole).isEqualTo(expectedRole);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // extractUserId()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractUserId()")
    class ExtractUserId {

        @Test
        @DisplayName("should extract the correct userId from a valid token")
        void extractUserId_validToken_returnsUserId() {
            // 1. Set up the test conditions
            String userId = "user-abc-123";
            String token  = jwtUtil.generateToken(userId, "USER");

            // 2. Run the method under test
            String extracted = jwtUtil.extractUserId(token);

            // 3. Verify the outcome
            assertThat(extracted).isEqualTo(userId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // extractRole()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractRole()")
    class ExtractRole {

        @Test
        @DisplayName("should extract USER role from token")
        void extractRole_userRole_returnsUser() {
            // 1. Set up the test conditions
            String token = jwtUtil.generateToken("user-001", "USER");

            // 2. Run the method under test
            String role = jwtUtil.extractRole(token);

            // 3. Verify the outcome
            assertThat(role).isEqualTo("USER");
        }

        @Test
        @DisplayName("should extract ADMIN role from token")
        void extractRole_adminRole_returnsAdmin() {
            // 1. Set up the test conditions
            String token = jwtUtil.generateToken("admin-001", "ADMIN");

            // 2. Run the method under test
            String role = jwtUtil.extractRole(token);

            // 3. Verify the outcome
            assertThat(role).isEqualTo("ADMIN");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("should return true for a freshly generated valid token")
        void validateToken_freshToken_returnsTrue() {
            // 1. Set up the test conditions
            String token = jwtUtil.generateToken("user-001", "USER");

            // 2. Run the method under test
            boolean valid = jwtUtil.validateToken(token);

            // 3. Verify the outcome
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should return false for a completely malformed string")
        void validateToken_malformedString_returnsFalse() {
            // 1. Set up the test conditions
            String badToken = "not.a.jwt";

            // 2. Run the method under test
            boolean valid = jwtUtil.validateToken(badToken);

            // 3. Verify the outcome
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should return false for an empty string")
        void validateToken_emptyString_returnsFalse() {
            // 1. Set up the test conditions
            String emptyToken = "";

            // 2. Run the method under test
            boolean valid = jwtUtil.validateToken(emptyToken);

            // 3. Verify the outcome
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should return false for a token signed with a different secret")
        void validateToken_differentSecret_returnsFalse() {
            // Arrange – generate a token with a DIFFERENT JwtUtil instance
            JwtUtil otherUtil = new JwtUtil();
            ReflectionTestUtils.setField(otherUtil, "secret",   "completely-different-secret-key-xyz!");
            ReflectionTestUtils.setField(otherUtil, "expiryMs", TEST_EXPIRY);
            String foreignToken = otherUtil.generateToken("user-001", "USER");

            // 2. Run the method under test
            boolean valid = jwtUtil.validateToken(foreignToken);

            // 3. Verify the outcome
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should return false for an expired token")
        void validateToken_expiredToken_returnsFalse() {
            // Arrange – create JwtUtil with a -1 ms expiry so token is immediately expired
            JwtUtil expiredUtil = new JwtUtil();
            ReflectionTestUtils.setField(expiredUtil, "secret",   TEST_SECRET);
            ReflectionTestUtils.setField(expiredUtil, "expiryMs", -1L);
            String expiredToken = expiredUtil.generateToken("user-001", "USER");

            // 2. Run the method under test
            boolean valid = jwtUtil.validateToken(expiredToken);

            // 3. Verify the outcome
            assertThat(valid).isFalse();
        }
    }
}
