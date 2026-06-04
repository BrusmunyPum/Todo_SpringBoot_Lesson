package com.example.todo.task.controller;

import com.example.todo.auth.service.CustomUserDetailsService;
import com.example.todo.common.exception.TaskAlreadyCompletedException;
import com.example.todo.common.exception.TaskNotFoundException;
import com.example.todo.common.security.JwtService;
import com.example.todo.task.dto.response.TaskPageResponse;
import com.example.todo.task.dto.response.TaskResponse;
import com.example.todo.task.entity.Task;
import com.example.todo.task.entity.TaskPriority;
import com.example.todo.task.mapper.TaskMapper;
import com.example.todo.task.service.TaskService;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @MockitoBean private TaskService taskService;
    @MockitoBean private TaskMapper taskMapper;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private AppUser testUser;
    private AppUser adminUser;
    private Task sampleTask;
    private TaskResponse sampleResponse;

    private static final String VALID_TOKEN       = "valid-test-token";
    private static final String ADMIN_TOKEN       = "admin-test-token";
    private static final String INVALID_TOKEN     = "invalid-test-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();

        testUser  = new AppUser("muny",  "muny@email.com",  "$2a$hashed");
        adminUser = new AppUser("admin", "admin@email.com", "$2a$hashed");
        adminUser.setRole(UserRole.ADMIN);

        sampleTask = new Task(
                "Buy groceries", false, TaskPriority.HIGH,
                LocalDate.of(2027, 1, 15), testUser
        );

        sampleResponse = new TaskResponse(
                1L, "Buy groceries", false, TaskPriority.HIGH,
                LocalDate.of(2027, 1, 15), 1L, "muny",
                Instant.now(), Instant.now()
        );

        // ── Regular user token ────────────────────────────────────────────────
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("muny");
        when(customUserDetailsService.loadUserByUsername("muny")).thenReturn(testUser);
        when(jwtService.isTokenValid(VALID_TOKEN, testUser)).thenReturn(true);

        // ── Admin user token ──────────────────────────────────────────────────
        when(jwtService.extractUsername(ADMIN_TOKEN)).thenReturn("admin");
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(adminUser);
        when(jwtService.isTokenValid(ADMIN_TOKEN, adminUser)).thenReturn(true);

        // ── Invalid token ─────────────────────────────────────────────────────
        when(jwtService.isTokenValid(eq(INVALID_TOKEN), any())).thenReturn(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/tasks  (admin-only)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/tasks")
    class GetAllTasks {

        @Test
        @DisplayName("should return 200 with paginated tasks when ADMIN")
        void shouldReturn200ForAdmin() throws Exception {
            Page<Task> taskPage = new PageImpl<>(List.of(sampleTask), PageRequest.of(0, 5), 1);
            TaskPageResponse pageResponse = new TaskPageResponse(
                    List.of(sampleResponse), 0, 5, 1L, 1, true, true
            );

            when(taskService.getAllTasks(any(), anyInt(), anyInt(), anyString(), anyString()))
                    .thenReturn(taskPage);
            when(taskMapper.toPageResponse(taskPage)).thenReturn(pageResponse);

            mockMvc.perform(
                    get("/api/v1/tasks")
                            .header("Authorization", "Bearer " + ADMIN_TOKEN)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Buy groceries"));
        }

        @Test
        @DisplayName("should return 403 when regular USER calls admin endpoint")
        void shouldReturn403ForRegularUser() throws Exception {
            mockMvc.perform(
                    get("/api/v1/tasks")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when no token is provided")
        void shouldReturn401WhenNoToken() throws Exception {
            mockMvc.perform(get("/api/v1/tasks"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 401 when token is invalid")
        void shouldReturn401WhenTokenIsInvalid() throws Exception {
            when(jwtService.extractUsername(INVALID_TOKEN)).thenReturn(null);

            mockMvc.perform(
                    get("/api/v1/tasks")
                            .header("Authorization", "Bearer " + INVALID_TOKEN)
            )
            .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/tasks/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/tasks/{id}")
    class GetTaskById {

        @Test
        @DisplayName("should return 200 with the task when it exists and user owns it")
        void shouldReturn200WhenFound() throws Exception {
            when(taskService.getTaskById(eq(1L), any(AppUser.class))).thenReturn(sampleTask);
            when(taskMapper.toResponse(sampleTask)).thenReturn(sampleResponse);

            mockMvc.perform(
                    get("/api/v1/tasks/1")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Buy groceries"))
            .andExpect(jsonPath("$.completed").value(false))
            .andExpect(jsonPath("$.priority").value("HIGH"));
        }

        @Test
        @DisplayName("should return 404 when task does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            when(taskService.getTaskById(eq(99L), any(AppUser.class)))
                    .thenThrow(new TaskNotFoundException(99L));

            mockMvc.perform(
                    get("/api/v1/tasks/99")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/tasks
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/tasks")
    class CreateTask {

        @Test
        @DisplayName("should return 201 Created with the new task")
        void shouldReturn201WhenCreated() throws Exception {
            when(taskService.createTask(any(AppUser.class), any())).thenReturn(sampleTask);
            when(taskMapper.toResponse(sampleTask)).thenReturn(sampleResponse);

            mockMvc.perform(
                    post("/api/v1/tasks")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Buy groceries",
                                      "priority": "HIGH",
                                      "dueDate": "2027-01-15"
                                    }
                                    """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Buy groceries"))
            .andExpect(jsonPath("$.completed").value(false));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void shouldReturn400WhenTitleIsBlank() throws Exception {
            mockMvc.perform(
                    post("/api/v1/tasks")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "",
                                      "priority": "HIGH"
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.title").exists());
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(
                    post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Buy groceries",
                                      "priority": "HIGH"
                                    }
                                    """)
            )
            .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/tasks/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("should return 204 No Content when deleted successfully")
        void shouldReturn204WhenDeleted() throws Exception {
            mockMvc.perform(
                    delete("/api/v1/tasks/1")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when task does not exist")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            doThrow(new TaskNotFoundException(99L))
                    .when(taskService).deleteTask(eq(99L), any(AppUser.class));

            mockMvc.perform(
                    delete("/api/v1/tasks/99")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/v1/tasks/{id}/complete
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/tasks/{id}/complete")
    class CompleteTask {

        @Test
        @DisplayName("should return 200 with completed=true")
        void shouldReturn200WhenCompleted() throws Exception {
            TaskResponse completedResponse = new TaskResponse(
                    1L, "Buy groceries", true, TaskPriority.HIGH,
                    LocalDate.of(2027, 1, 15), 1L, "muny",
                    Instant.now(), Instant.now()
            );
            when(taskService.completeTask(eq(1L), any(AppUser.class))).thenReturn(sampleTask);
            when(taskMapper.toResponse(sampleTask)).thenReturn(completedResponse);

            mockMvc.perform(
                    patch("/api/v1/tasks/1/complete")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true));
        }

        @Test
        @DisplayName("should return 409 when task is already completed")
        void shouldReturn409WhenAlreadyCompleted() throws Exception {
            when(taskService.completeTask(eq(1L), any(AppUser.class)))
                    .thenThrow(new TaskAlreadyCompletedException(1L));

            mockMvc.perform(
                    patch("/api/v1/tasks/1/complete")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/v1/tasks/{id}/reopen
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/tasks/{id}/reopen")
    class ReopenTask {

        @Test
        @DisplayName("should return 200 with completed=false after reopen")
        void shouldReturn200WhenReopened() throws Exception {
            TaskResponse reopenedResponse = new TaskResponse(
                    1L, "Buy groceries", false, TaskPriority.HIGH,
                    LocalDate.of(2027, 1, 15), 1L, "muny",
                    Instant.now(), Instant.now()
            );
            when(taskService.reopenTask(eq(1L), any(AppUser.class))).thenReturn(sampleTask);
            when(taskMapper.toResponse(sampleTask)).thenReturn(reopenedResponse);

            mockMvc.perform(
                    patch("/api/v1/tasks/1/reopen")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(false));
        }
    }
}
