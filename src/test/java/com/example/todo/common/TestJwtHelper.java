package com.example.todo.common;

import com.example.todo.common.security.JwtService;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.entity.UserRole;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Helper class for generating JWT tokens in controller tests.
 *
 * Controller tests need a real Bearer token to pass through JwtAuthFilter.
 * We use the real JwtService with the same secret as application.properties
 * so the filter accepts the token.
 */
public class TestJwtHelper {

    private static final String TEST_SECRET =
            "CdJeCU+qbKJVbIGBZrepVjuT9wmfbw9Mqmz2O4jo0+c=";
    private static final long TEST_EXPIRATION = 86400000L; // 24h

    /**
     * Generate a valid JWT token for a user with USER role.
     * Use this for most controller tests.
     */
    public static String generateUserToken(String username) {
        return generateToken(username, UserRole.USER);
    }

    /**
     * Generate a valid JWT token for a user with ADMIN role.
     * Use this for admin-only endpoint tests.
     */
    public static String generateAdminToken(String username) {
        return generateToken(username, UserRole.ADMIN);
    }

    private static String generateToken(String username, UserRole role) {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);

        AppUser user = new AppUser(username, username + "@email.com", "$2a$hashed");
        user.setRole(role);

        return jwtService.generateToken(user);
    }
}
