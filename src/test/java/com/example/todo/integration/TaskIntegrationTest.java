package com.example.todo.integration;

import com.example.todo.task.repository.TaskRepository;
import com.example.todo.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ─── True Integration Test ────────────────────────────────────────────────────
// NO @MockitoBean — everything is real:
//   real controllers, real services, real repositories, real PostgreSQL.
//
// This tests that ALL layers work correctly together.
// Unit tests prove each piece is correct in isolation.
// Integration tests prove the pieces connect correctly.
// ─────────────────────────────────────────────────────────────────────────────
@SpringBootTest
@ActiveProfiles("test")
class TaskIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private static final String USERNAME = "integration_user";
    private static final String EMAIL    = "integration@test.com";
    private static final String PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();

        new TransactionTemplate(transactionManager).execute(status -> {
            taskRepository.deleteAll();
            userRepository.deleteByUsername(USERNAME);
            return null;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Full happy-path flow:
    // Register → Login → Create Task → Get Task → Complete Task → Delete
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full flow: register → login → create task → complete task → delete")
    void shouldCompleteFullTaskLifecycle() throws Exception {

        // ── Step 1: Register ──────────────────────────────────────────────────
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, EMAIL, PASSWORD))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value(USERNAME));

        assertThat(userRepository.existsByUsername(USERNAME)).isTrue();

        // ── Step 2: Login — get a real JWT token ──────────────────────────────
        MvcResult loginResult = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, PASSWORD))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        // ── Step 3: Verify /me resolves the authenticated user ────────────────
        mockMvc.perform(
                get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(USERNAME));

        // ── Step 4: Create a task — owner is set from JWT, not request body ───
        MvcResult createResult = mockMvc.perform(
                post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Buy groceries",
                                  "priority": "HIGH",
                                  "dueDate": "2027-12-31"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Buy groceries"))
        .andExpect(jsonPath("$.completed").value(false))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andReturn();

        long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        assertThat(taskRepository.findById(taskId)).isPresent();

        // ── Step 5: Get the task by ID ────────────────────────────────────────
        mockMvc.perform(
                get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.title").value("Buy groceries"))
        .andExpect(jsonPath("$.completed").value(false));

        // ── Step 6: Complete the task ─────────────────────────────────────────
        mockMvc.perform(
                patch("/api/v1/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completed").value(true));

        assertThat(taskRepository.findById(taskId))
                .isPresent()
                .get()
                .matches(task -> task.isCompleted(), "task should be completed");

        // ── Step 7: Complete again — should get 409 Conflict ─────────────────
        mockMvc.perform(
                patch("/api/v1/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));

        // ── Step 8: Reopen the task ───────────────────────────────────────────
        mockMvc.perform(
                patch("/api/v1/tasks/" + taskId + "/reopen")
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completed").value(false));

        // ── Step 9: Delete the task ───────────────────────────────────────────
        mockMvc.perform(
                delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNoContent());

        assertThat(taskRepository.findById(taskId)).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security integration: protected endpoints reject unauthenticated requests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 401 for all protected endpoints when no token provided")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"test\",\"priority\":\"LOW\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation integration: invalid request body returns 400
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 400 with validation errors for invalid register request")
    void shouldRejectInvalidRegistration() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "email": "not-an-email",
                                  "password": "123"
                                }
                                """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors.username").exists())
        .andExpect(jsonPath("$.errors.email").exists())
        .andExpect(jsonPath("$.errors.password").exists());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Duplicate registration
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 409 Conflict when registering with duplicate username")
    void shouldRejectDuplicateUsername() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, EMAIL, PASSWORD))
        )
        .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "different@email.com",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, PASSWORD))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wrong credentials
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 401 with same message for wrong username and wrong password")
    void shouldRejectWrongCredentials() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, EMAIL, PASSWORD))
        )
        .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "wrongpassword"
                                }
                                """.formatted(USERNAME))
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "nobody",
                                  "password": "%s"
                                }
                                """.formatted(PASSWORD))
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
