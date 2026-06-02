package com.example.todo.common.security;

import com.example.todo.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    // Valid 256-bit Base64-encoded secret — same as application.properties
    private static final String TEST_SECRET =
            "CdJeCU+qbKJVbIGBZrepVjuT9wmfbw9Mqmz2O4jo0+c=";

    private static final long TEST_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Spring's @Value injection does NOT run in plain unit tests.
        // ReflectionTestUtils lets us set private fields directly for testing.
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);
    }

    @Test
    @DisplayName("generateToken() should return a non-blank JWT string")
    void shouldGenerateNonBlankToken() {
        // ── Arrange ───────────────────────────────────────────────────────
        AppUser user = new AppUser("muny", "muny@email.com", "hashed");

        // ── Act ───────────────────────────────────────────────────────────
        String token = jwtService.generateToken(user);

        // ── Assert ────────────────────────────────────────────────────────
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        // Every JWT has exactly 3 parts: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername() should return the username embedded in the token")
    void shouldExtractCorrectUsername() {
        // ── Arrange ───────────────────────────────────────────────────────
        AppUser user = new AppUser("muny", "muny@email.com", "hashed");
        String token = jwtService.generateToken(user);

        // ── Act ───────────────────────────────────────────────────────────
        String extractedUsername = jwtService.extractUsername(token);

        // ── Assert ────────────────────────────────────────────────────────
        assertThat(extractedUsername).isEqualTo("muny");
    }

    @Test
    @DisplayName("isTokenValid() should return true for a valid, non-expired token")
    void shouldReturnTrueForValidToken() {
        // ── Arrange ───────────────────────────────────────────────────────
        AppUser user = new AppUser("muny", "muny@email.com", "hashed");
        String token = jwtService.generateToken(user);

        // ── Act ───────────────────────────────────────────────────────────
        boolean isValid = jwtService.isTokenValid(token, user);

        // ── Assert ────────────────────────────────────────────────────────
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() should return false when username does not match the token")
    void shouldReturnFalseWhenUsernameDoesNotMatch() {
        // ── Arrange ───────────────────────────────────────────────────────
        AppUser user1 = new AppUser("muny", "muny@email.com", "hashed");
        AppUser user2 = new AppUser("other", "other@email.com", "hashed");

        // Token belongs to user1
        String token = jwtService.generateToken(user1);

        // ── Act: validate against user2 ───────────────────────────────────
        boolean isValid = jwtService.isTokenValid(token, user2);

        // ── Assert ────────────────────────────────────────────────────────
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() should return false when token is expired")
    void shouldReturnFalseForExpiredToken() {
        // ── Arrange: set expiration to 1ms so it expires instantly ───────
        ReflectionTestUtils.setField(jwtService, "expiration", 1L);

        AppUser user = new AppUser("muny", "muny@email.com", "hashed");
        String token = jwtService.generateToken(user);

        // ── Act ───────────────────────────────────────────────────────────
        boolean isValid = jwtService.isTokenValid(token, user);

        // ── Assert ────────────────────────────────────────────────────────
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("extractUsername() should throw when given a malformed token")
    void shouldThrowForMalformedToken() {
        // ── Act & Assert ──────────────────────────────────────────────────
        // Not a valid JWT — should throw an exception
        assertThatThrownBy(() -> jwtService.extractUsername("this.is.not.a.valid.jwt"))
                .isInstanceOf(Exception.class); // any runtime exception
    }
}
