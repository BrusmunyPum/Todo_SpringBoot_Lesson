package com.example.todo.auth.controller;

import com.example.todo.auth.dto.response.LoginResponse;
import com.example.todo.auth.service.AuthService;
import com.example.todo.common.exception.DuplicateResourceException;
import com.example.todo.common.exception.InvalidCredentialsException;
import com.example.todo.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ─── Why @SpringBootTest instead of @WebMvcTest ───────────────────────────────
// @WebMvcTest is in spring-boot-test-autoconfigure which is not available as a
// standalone artifact in Spring Boot 4.0.x via spring-boot-starter-webmvc-test.
// @SpringBootTest loads the full context. We then build MockMvc manually from
// the WebApplicationContext — this applies all filters including Spring Security.
// ─────────────────────────────────────────────────────────────────────────────
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    // WebApplicationContext lets us build MockMvc with all real filters
    @Autowired
    private WebApplicationContext context;

    // FilterChainProxy is Spring Security's main filter bean.
    // We inject it by name — Spring registers it as "springSecurityFilterChain".
    // Adding it to MockMvc applies the full security filter chain to every request.
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)   // ← applies Spring Security
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/register
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 Created with user info when registration succeeds")
        void shouldReturn201WhenRegistrationSucceeds() throws Exception {
            AppUser savedUser = new AppUser("muny", "muny@email.com", "$2a$hashed");
            when(authService.register(any())).thenReturn(savedUser);

            mockMvc.perform(
                    post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "muny",
                                      "email": "muny@email.com",
                                      "password": "password123"
                                    }
                                    """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("muny"))
            .andExpect(jsonPath("$.email").value("muny@email.com"))
            .andExpect(jsonPath("$.password").doesNotExist()); // never expose password
        }

        @Test
        @DisplayName("should return 400 when username is blank")
        void shouldReturn400WhenUsernameIsBlank() throws Exception {
            mockMvc.perform(
                    post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "",
                                      "email": "muny@email.com",
                                      "password": "password123"
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.username").exists());
        }

        @Test
        @DisplayName("should return 400 when username is too short")
        void shouldReturn400WhenUsernameIsTooShort() throws Exception {
            mockMvc.perform(
                    post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "ab",
                                      "email": "muny@email.com",
                                      "password": "password123"
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.username").exists());
        }

        @Test
        @DisplayName("should return 400 when email format is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            mockMvc.perform(
                    post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "muny",
                                      "email": "not-an-email",
                                      "password": "password123"
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            mockMvc.perform(
                    post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "muny",
                                      "email": "muny@email.com",
                                      "password": "abc"
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").exists());
        }

        @Test
        @DisplayName("should return 409 Conflict when username already exists")
        void shouldReturn409WhenUsernameIsDuplicate() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new DuplicateResourceException("Username already exists"));

            mockMvc.perform(
                    post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "muny",
                                      "email": "muny@email.com",
                                      "password": "password123"
                                    }
                                    """)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Username already exists"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/login
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 OK with a token when credentials are valid")
        void shouldReturn200WithTokenOnValidLogin() throws Exception {
            when(authService.login(any()))
                    .thenReturn(new LoginResponse("fake.jwt.token", 86400L));

            mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "muny",
                                      "password": "password123"
                                    }
                                    """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("fake.jwt.token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(86400));
        }

        @Test
        @DisplayName("should return 401 when credentials are wrong")
        void shouldReturn401WhenCredentialsAreWrong() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new InvalidCredentialsException("Invalid username or password"));

            mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "muny",
                                      "password": "wrongpassword"
                                    }
                                    """)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("should return 400 when username is missing")
        void shouldReturn400WhenUsernameIsMissing() throws Exception {
            mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "password": "password123"
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.username").exists());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/logout
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("should return 204 No Content")
        void shouldReturn204() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isNoContent());
        }
    }
}
