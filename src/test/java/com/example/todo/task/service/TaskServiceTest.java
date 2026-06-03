package com.example.todo.task.service;

import com.example.todo.common.exception.TaskAlreadyCompletedException;
import com.example.todo.common.exception.TaskNotFoundException;
import com.example.todo.task.dto.request.CreateTaskRequest;
import com.example.todo.task.dto.request.PatchTaskRequest;
import com.example.todo.task.dto.request.UpdateTaskRequest;
import com.example.todo.task.entity.Task;
import com.example.todo.task.entity.TaskPriority;
import com.example.todo.task.repository.TaskRepository;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskService taskService;

    // ── Shared test data ──────────────────────────────────────────────────────
    // We build these once and reuse them across tests.
    // Each @Test gets a fresh copy because Mockito resets mocks between tests.

    private AppUser testUser;
    private Task incompleteTask;
    private Task completedTask;

    @BeforeEach
    void setUp() {
        testUser = new AppUser("muny", "muny@email.com", "$2a$hashed");

        incompleteTask = new Task(
                "Buy groceries",
                false,                    // not completed
                TaskPriority.MEDIUM,
                LocalDate.of(2026, 12, 31),
                testUser
        );

        completedTask = new Task(
                "Read book",
                true,                     // already completed
                TaskPriority.LOW,
                null,
                testUser
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTaskById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTaskById()")
    class GetTaskById {

        @Test
        @DisplayName("should return the task when it exists")
        void shouldReturnTaskWhenFound() {
            // ── Arrange ───────────────────────────────────────────────────
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

            // ── Act ───────────────────────────────────────────────────────
            Task result = taskService.getTaskById(1L);

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.getTitle()).isEqualTo("Buy groceries");
            assertThat(result.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when task does not exist")
        void shouldThrowWhenTaskNotFound() {
            // ── Arrange ───────────────────────────────────────────────────
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> taskService.getTaskById(99L))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTask()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Captor
        ArgumentCaptor<Task> taskCaptor;

        @Test
        @DisplayName("should create task with correct fields and always start as incomplete")
        void shouldCreateTaskSuccessfully() {
            // ── Arrange ───────────────────────────────────────────────────
            CreateTaskRequest request = new CreateTaskRequest(
                    "Buy groceries",
                    TaskPriority.HIGH,
                    LocalDate.of(2026, 12, 31),
                    1L   // userId
            );

            when(userService.getUserById(1L)).thenReturn(testUser);
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            Task result = taskService.createTask(request);

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.getTitle()).isEqualTo("Buy groceries");
            assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("should always set completed=false on creation, regardless of request")
        void shouldAlwaysCreateAsIncomplete() {
            // ── Arrange ───────────────────────────────────────────────────
            CreateTaskRequest request = new CreateTaskRequest(
                    "New task", TaskPriority.MEDIUM, null, 1L
            );

            when(userService.getUserById(1L)).thenReturn(testUser);
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            taskService.createTask(request);

            // ── Assert with Captor ────────────────────────────────────────
            verify(taskRepository).save(taskCaptor.capture());
            Task savedTask = taskCaptor.getValue();

            // Business rule: new tasks MUST start as incomplete
            assertThat(savedTask.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should assign the correct user to the task")
        void shouldAssignCorrectUser() {
            // ── Arrange ───────────────────────────────────────────────────
            CreateTaskRequest request = new CreateTaskRequest(
                    "Task for muny", TaskPriority.LOW, null, 1L
            );

            when(userService.getUserById(1L)).thenReturn(testUser);
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            taskService.createTask(request);

            // ── Assert ────────────────────────────────────────────────────
            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getUser().getUsername()).isEqualTo("muny");
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when the userId does not exist")
        void shouldThrowWhenUserNotFound() {
            // ── Arrange ───────────────────────────────────────────────────
            CreateTaskRequest request = new CreateTaskRequest(
                    "Task", TaskPriority.LOW, null, 999L
            );

            // UserService throws when user not found
            when(userService.getUserById(999L))
                    .thenThrow(new TaskNotFoundException(999L));

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> taskService.createTask(request))
                    .isInstanceOf(TaskNotFoundException.class);

            // Task must never be saved if user doesn't exist
            verify(taskRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // completeTask()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {

        @Test
        @DisplayName("should mark task as completed when it is not already done")
        void shouldCompleteTask() {
            // ── Arrange ───────────────────────────────────────────────────
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            Task result = taskService.completeTask(1L);

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should throw TaskAlreadyCompletedException when task is already done")
        void shouldThrowWhenAlreadyCompleted() {
            // ── Arrange ───────────────────────────────────────────────────
            // completedTask.isCompleted() == true
            when(taskRepository.findById(2L)).thenReturn(Optional.of(completedTask));

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> taskService.completeTask(2L))
                    .isInstanceOf(TaskAlreadyCompletedException.class);

            // save() must never be called — we threw before reaching it
            verify(taskRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reopenTask()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reopenTask()")
    class ReopenTask {

        @Test
        @DisplayName("should set completed=false when reopening a completed task")
        void shouldReopenTask() {
            // ── Arrange ───────────────────────────────────────────────────
            when(taskRepository.findById(2L)).thenReturn(Optional.of(completedTask));
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            Task result = taskService.reopenTask(2L);

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.isCompleted()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteTask()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTask()")
    class DeleteTask {

        @Test
        @DisplayName("should call repository.delete() with the correct task")
        void shouldDeleteTask() {
            // ── Arrange ───────────────────────────────────────────────────
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

            // ── Act ───────────────────────────────────────────────────────
            // deleteTask() is void — nothing to assert on the return value.
            // We verify the behavior: was delete() called with the right object?
            taskService.deleteTask(1L);

            // ── Assert (via verify) ───────────────────────────────────────
            verify(taskRepository, times(1)).delete((Task) incompleteTask);
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when deleting a non-existent task")
        void shouldThrowWhenTaskNotFound() {
            // ── Arrange ───────────────────────────────────────────────────
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> taskService.deleteTask(99L))
                    .isInstanceOf(TaskNotFoundException.class);

            // delete() must never be called if the task doesn't exist
            verify(taskRepository, never()).delete(any(Task.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateTask() — PUT (full replace)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTask()")
    class UpdateTask {

        @Test
        @DisplayName("should update all fields of the task")
        void shouldUpdateAllFields() {
            // ── Arrange ───────────────────────────────────────────────────
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Updated title",
                    true,
                    TaskPriority.HIGH,
                    LocalDate.of(2027, 1, 1)
            );

            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            Task result = taskService.updateTask(1L, request);

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.getTitle()).isEqualTo("Updated title");
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // patchTask() — PATCH (partial update)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patchTask()")
    class PatchTask {

        @Test
        @DisplayName("should only update fields that are non-null in the request")
        void shouldOnlyUpdateNonNullFields() {
            // ── Arrange ───────────────────────────────────────────────────
            // Only title is set — priority and dueDate are null
            PatchTaskRequest request = new PatchTaskRequest();
            request.setTitle("Patched title");
            // priority = null (not provided)
            // dueDate  = null (not provided)

            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            Task result = taskService.patchTask(1L, request);

            // ── Assert ────────────────────────────────────────────────────
            // Title was updated
            assertThat(result.getTitle()).isEqualTo("Patched title");
            // Priority kept original value — NOT overwritten by null
            assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM);
            // DueDate kept original value
            assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllTasks() — Pagination + Sort validation (private method tested here)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test
        @DisplayName("should return paginated tasks when no completed filter is applied")
        void shouldReturnPagedTasks() {
            // ── Arrange ───────────────────────────────────────────────────
            // PageImpl is the concrete class that implements Page<T>
            Page<Task> fakePage = new PageImpl<>(
                    List.of(incompleteTask, completedTask),
                    PageRequest.of(0, 5),
                    2  // total elements
            );

            when(taskRepository.findAll(any(Pageable.class))).thenReturn(fakePage);

            // ── Act ───────────────────────────────────────────────────────
            Page<Task> result = taskService.getAllTasks(null, 0, 5, "id", "asc");

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getNumber()).isEqualTo(0);     // page number
            assertThat(result.getSize()).isEqualTo(5);        // page size
        }

        @Test
        @DisplayName("should filter by completed=true when filter is applied")
        void shouldFilterByCompleted() {
            // ── Arrange ───────────────────────────────────────────────────
            Page<Task> fakePage = new PageImpl<>(
                    List.of(completedTask),
                    PageRequest.of(0, 5),
                    1
            );

            when(taskRepository.findByCompleted(eq(true), any(Pageable.class)))
                    .thenReturn(fakePage);

            // ── Act ───────────────────────────────────────────────────────
            Page<Task> result = taskService.getAllTasks(true, 0, 5, "id", "asc");

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isCompleted()).isTrue();

            // findByCompleted must be called, NOT findAll
            verify(taskRepository).findByCompleted(eq(true), any(Pageable.class));
            verify(taskRepository, never()).findAll(any(Pageable.class));
        }

        // ── Testing the private validateSortBy() indirectly ──────────────────
        // We cannot call validateSortBy() directly because it's private.
        // But getAllTasks() calls it — so passing a bad sort field exercises it.

        @Test
        @DisplayName("should throw IllegalArgumentException for an invalid sort field")
        void shouldThrowForInvalidSortField() {
            assertThatThrownBy(() ->
                    taskService.getAllTasks(null, 0, 5, "invalidField", "asc")
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sort field");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for an invalid sort direction")
        void shouldThrowForInvalidSortDirection() {
            assertThatThrownBy(() ->
                    taskService.getAllTasks(null, 0, 5, "id", "sideways")
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sort direction");
        }

        @Test
        @DisplayName("should accept all valid sort fields without throwing")
        void shouldAcceptAllValidSortFields() {
            Page<Task> emptyPage = new PageImpl<>(List.of());
            when(taskRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // These are all the valid fields declared in validateSortBy()
            List<String> validFields = List.of(
                    "id", "title", "completed", "priority", "dueDate",
                    "createdAt", "updatedAt"
            );

            for (String field : validFields) {
                // Should not throw
                taskService.getAllTasks(null, 0, 5, field, "asc");
            }
        }
    }
}
