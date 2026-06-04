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
import org.springframework.test.util.ReflectionTestUtils;
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
import org.springframework.security.access.AccessDeniedException;

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

    private AppUser testUser;
    private AppUser otherUser;
    private Task incompleteTask;
    private Task completedTask;

    @BeforeEach
    void setUp() {
        testUser  = new AppUser("muny",  "muny@email.com",  "$2a$hashed");
        otherUser = new AppUser("other", "other@email.com", "$2a$hashed");

        // IDs are assigned by the database, so we set them manually for unit tests
        ReflectionTestUtils.setField(testUser,  "id", 1L);
        ReflectionTestUtils.setField(otherUser, "id", 2L);

        incompleteTask = new Task(
                "Buy groceries",
                false,
                TaskPriority.MEDIUM,
                LocalDate.of(2026, 12, 31),
                testUser
        );

        completedTask = new Task(
                "Read book",
                true,
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
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

            Task result = taskService.getTaskById(1L);

            assertThat(result.getTitle()).isEqualTo("Buy groceries");
            assertThat(result.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when task does not exist")
        void shouldThrowWhenTaskNotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(99L))
                    .isInstanceOf(TaskNotFoundException.class);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when task belongs to another user")
        void shouldThrowWhenNotOwner() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

            assertThatThrownBy(() -> taskService.getTaskById(1L, otherUser))
                    .isInstanceOf(AccessDeniedException.class);
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
            CreateTaskRequest request = new CreateTaskRequest(
                    "Buy groceries", TaskPriority.HIGH, LocalDate.of(2026, 12, 31)
            );

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.createTask(testUser, request);

            assertThat(result.getTitle()).isEqualTo("Buy groceries");
            assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("should always set completed=false on creation")
        void shouldAlwaysCreateAsIncomplete() {
            CreateTaskRequest request = new CreateTaskRequest("New task", TaskPriority.MEDIUM, null);

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            taskService.createTask(testUser, request);

            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should assign the authenticated user to the task")
        void shouldAssignCurrentUser() {
            CreateTaskRequest request = new CreateTaskRequest("Task for muny", TaskPriority.LOW, null);

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            taskService.createTask(testUser, request);

            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getUser().getUsername()).isEqualTo("muny");
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
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.completeTask(1L, testUser);

            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should throw TaskAlreadyCompletedException when already done")
        void shouldThrowWhenAlreadyCompleted() {
            when(taskRepository.findById(2L)).thenReturn(Optional.of(completedTask));

            assertThatThrownBy(() -> taskService.completeTask(2L, testUser))
                    .isInstanceOf(TaskAlreadyCompletedException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when task belongs to another user")
        void shouldThrowWhenNotOwner() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

            assertThatThrownBy(() -> taskService.completeTask(1L, otherUser))
                    .isInstanceOf(AccessDeniedException.class);
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
            when(taskRepository.findById(2L)).thenReturn(Optional.of(completedTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.reopenTask(2L, testUser);

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
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

            taskService.deleteTask(1L, testUser);

            verify(taskRepository, times(1)).delete(incompleteTask);
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when deleting a non-existent task")
        void shouldThrowWhenTaskNotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(99L, testUser))
                    .isInstanceOf(TaskNotFoundException.class);

            verify(taskRepository, never()).delete(any(Task.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when task belongs to another user")
        void shouldThrowWhenNotOwner() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

            assertThatThrownBy(() -> taskService.deleteTask(1L, otherUser))
                    .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).delete(any(Task.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateTask() — PUT
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTask()")
    class UpdateTask {

        @Test
        @DisplayName("should update all fields of the task")
        void shouldUpdateAllFields() {
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Updated title", true, TaskPriority.HIGH, LocalDate.of(2027, 1, 1)
            );

            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.updateTask(1L, request, testUser);

            assertThat(result.getTitle()).isEqualTo("Updated title");
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // patchTask() — PATCH
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patchTask()")
    class PatchTask {

        @Test
        @DisplayName("should only update fields that are non-null in the request")
        void shouldOnlyUpdateNonNullFields() {
            PatchTaskRequest request = new PatchTaskRequest();
            request.setTitle("Patched title");

            when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.patchTask(1L, request, testUser);

            assertThat(result.getTitle()).isEqualTo("Patched title");
            assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM);
            assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllTasks()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test
        @DisplayName("should return paginated tasks when no completed filter is applied")
        void shouldReturnPagedTasks() {
            Page<Task> fakePage = new PageImpl<>(
                    List.of(incompleteTask, completedTask), PageRequest.of(0, 5), 2
            );

            when(taskRepository.findAll(any(Pageable.class))).thenReturn(fakePage);

            Page<Task> result = taskService.getAllTasks(null, 0, 5, "id", "asc");

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should filter by completed=true when filter is applied")
        void shouldFilterByCompleted() {
            Page<Task> fakePage = new PageImpl<>(List.of(completedTask), PageRequest.of(0, 5), 1);

            when(taskRepository.findByCompleted(eq(true), any(Pageable.class))).thenReturn(fakePage);

            Page<Task> result = taskService.getAllTasks(true, 0, 5, "id", "asc");

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isCompleted()).isTrue();
            verify(taskRepository).findByCompleted(eq(true), any(Pageable.class));
            verify(taskRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for an invalid sort field")
        void shouldThrowForInvalidSortField() {
            assertThatThrownBy(() -> taskService.getAllTasks(null, 0, 5, "invalidField", "asc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sort field");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for an invalid sort direction")
        void shouldThrowForInvalidSortDirection() {
            assertThatThrownBy(() -> taskService.getAllTasks(null, 0, 5, "id", "sideways"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sort direction");
        }

        @Test
        @DisplayName("should accept all valid sort fields without throwing")
        void shouldAcceptAllValidSortFields() {
            Page<Task> emptyPage = new PageImpl<>(List.of());
            when(taskRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            List<String> validFields = List.of(
                    "id", "title", "completed", "priority", "dueDate", "createdAt", "updatedAt"
            );
            for (String field : validFields) {
                taskService.getAllTasks(null, 0, 5, field, "asc");
            }
        }
    }
}
