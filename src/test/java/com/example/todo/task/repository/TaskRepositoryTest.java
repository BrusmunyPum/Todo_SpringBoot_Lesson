package com.example.todo.task.repository;

import com.example.todo.task.entity.Task;
import com.example.todo.task.entity.TaskPriority;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// ─── Why @SpringBootTest instead of @DataJpaTest ─────────────────────────────
// @DataJpaTest requires spring-boot-test-autoconfigure artifacts that are not
// yet available as standalone starters in Spring Boot 4.0.x.
// @SpringBootTest loads the full context (slower) but gives us the same thing
// we need: real repositories wired to a real PostgreSQL database.
// ─────────────────────────────────────────────────────────────────────────────

@SpringBootTest

// Use application-test.properties → connects to todo_test_db (not todo_db)
@ActiveProfiles("test")

// @Transactional on the class: every @Test method runs in its own transaction
// that automatically ROLLS BACK when the test finishes.
// This means test data never accumulates — every test starts with a clean state.
@Transactional
class TaskRepositoryTest {

    // Real beans — no mocks. @SpringBootTest wires these to the real database.
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    // ── Shared test data ──────────────────────────────────────────────────────
    private AppUser userMuny;
    private AppUser userOther;

    @BeforeEach
    void setUp() {
        // Save users first — tasks have a FK to users
        userMuny  = userRepository.save(new AppUser("muny",  "muny@email.com",  "$2a$hashed"));
        userOther = userRepository.save(new AppUser("other", "other@email.com", "$2a$hashed"));

        // Save tasks — 3 for muny, 1 for other
        taskRepository.save(new Task("Buy groceries", false, TaskPriority.HIGH,   LocalDate.of(2027, 1, 15), userMuny));
        taskRepository.save(new Task("Read a book",   true,  TaskPriority.LOW,    null,                      userMuny));
        taskRepository.save(new Task("Morning run",   false, TaskPriority.MEDIUM, LocalDate.of(2027, 3, 10), userMuny));
        taskRepository.save(new Task("Other task",    false, TaskPriority.HIGH,   LocalDate.of(2027, 2, 20), userOther));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByCompleted()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByCompleted()")
    class FindByCompleted {

        @Test
        @DisplayName("should return only completed tasks")
        void shouldReturnOnlyCompletedTasks() {
            Page<Task> result = taskRepository.findByCompleted(true, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Read a book");
            assertThat(result.getContent().get(0).isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should return only incomplete tasks")
        void shouldReturnOnlyIncompleteTasks() {
            Page<Task> result = taskRepository.findByCompleted(false, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent()).allMatch(task -> !task.isCompleted());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByUserId()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByUserId()")
    class FindByUserId {

        @Test
        @DisplayName("should return only tasks belonging to the given user")
        void shouldReturnOnlyUserTasks() {
            Page<Task> result = taskRepository.findByUserId(userMuny.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent())
                    .allMatch(task -> task.getUser().getId().equals(userMuny.getId()));
        }

        @Test
        @DisplayName("should return tasks for other user separately")
        void shouldReturnTasksForOtherUserSeparately() {
            Page<Task> result = taskRepository.findByUserId(userOther.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Other task");
        }

        @Test
        @DisplayName("should return empty page when user has no tasks")
        void shouldReturnEmptyWhenUserHasNoTasks() {
            AppUser emptyUser = userRepository.save(
                    new AppUser("nobody", "nobody@email.com", "$2a$hashed")
            );

            Page<Task> result = taskRepository.findByUserId(emptyUser.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByUserIdAndCompleted()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByUserIdAndCompleted()")
    class FindByUserIdAndCompleted {

        @Test
        @DisplayName("should return only completed tasks for the given user")
        void shouldReturnCompletedTasksForUser() {
            Page<Task> result = taskRepository.findByUserIdAndCompleted(
                    userMuny.getId(), true, PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Read a book");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // countByUserId()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("countByUserId()")
    class CountByUserId {

        @Test
        @DisplayName("should return the correct count of tasks for a user")
        void shouldReturnCorrectCount() {
            long count = taskRepository.countByUserId(userMuny.getId());
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should return 0 when user has no tasks")
        void shouldReturnZeroForUserWithNoTasks() {
            AppUser emptyUser = userRepository.save(
                    new AppUser("nobody", "nobody@email.com", "$2a$hashed")
            );
            long count = taskRepository.countByUserId(emptyUser.getId());
            assertThat(count).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TaskSpecifications — dynamic filtering
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TaskSpecifications.withFilters()")
    class Specifications {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("should filter by title — partial, case-insensitive match")
        void shouldFilterByTitle() {
            var spec = TaskSpecifications.withFilters("buy", null, null, null, null, null);
            Page<Task> result = taskRepository.findAll(spec, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should filter by title case-insensitively — BUY matches Buy groceries")
        void shouldFilterByTitleCaseInsensitive() {
            var spec = TaskSpecifications.withFilters("BUY", null, null, null, null, null);
            Page<Task> result = taskRepository.findAll(spec, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should filter by completed status")
        void shouldFilterByCompleted() {
            var spec = TaskSpecifications.withFilters(null, true, null, null, null, null);
            Page<Task> result = taskRepository.findAll(spec, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should filter by priority HIGH — returns 2 tasks")
        void shouldFilterByPriority() {
            var spec = TaskSpecifications.withFilters(null, null, TaskPriority.HIGH, null, null, null);
            Page<Task> result = taskRepository.findAll(spec, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(t -> t.getPriority() == TaskPriority.HIGH);
        }

        @Test
        @DisplayName("should filter by dueAfter — only tasks on or after the date")
        void shouldFilterByDueAfter() {
            var spec = TaskSpecifications.withFilters(
                    null, null, null, LocalDate.of(2027, 3, 1), null, null
            );
            Page<Task> result = taskRepository.findAll(spec, pageable);

            // Only "Morning run" (2027-03-10) qualifies
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Morning run");
        }

        @Test
        @DisplayName("should filter by dueBefore — only tasks on or before the date")
        void shouldFilterByDueBefore() {
            var spec = TaskSpecifications.withFilters(
                    null, null, null, null, LocalDate.of(2027, 2, 1), null
            );
            Page<Task> result = taskRepository.findAll(spec, pageable);

            // Only "Buy groceries" (2027-01-15) qualifies
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should filter by exact dueDate")
        void shouldFilterByExactDueDate() {
            var spec = TaskSpecifications.withFilters(
                    null, null, null, null, null, LocalDate.of(2027, 3, 10)
            );
            Page<Task> result = taskRepository.findAll(spec, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Morning run");
        }

        @Test
        @DisplayName("should combine dueAfter and dueBefore as a date range")
        void shouldFilterByDateRange() {
            // Range: 2027-01-01 to 2027-02-28
            var spec = TaskSpecifications.withFilters(
                    null, null, null,
                    LocalDate.of(2027, 1, 1),   // dueAfter
                    LocalDate.of(2027, 2, 28),  // dueBefore
                    null
            );
            Page<Task> result = taskRepository.findAll(spec, pageable);

            // "Buy groceries" (Jan 15) and "Other task" (Feb 20) qualify
            // "Morning run" (Mar 10) does not
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("should combine title + priority filters")
        void shouldCombineFilters() {
            var spec = TaskSpecifications.withFilters(
                    "task", null, TaskPriority.HIGH, null, null, null
            );
            Page<Task> result = taskRepository.findAll(spec, pageable);

            // Only "Other task" matches both title "task" AND priority HIGH
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Other task");
        }

        @Test
        @DisplayName("should return all tasks when all filters are null")
        void shouldReturnAllTasksWhenNoFilters() {
            var spec = TaskSpecifications.withFilters(null, null, null, null, null, null);
            Page<Task> result = taskRepository.findAll(spec, pageable);

            assertThat(result.getContent()).hasSize(4);
        }

        @Test
        @DisplayName("should return empty when no tasks match the filter")
        void shouldReturnEmptyWhenNoMatch() {
            var spec = TaskSpecifications.withFilters("xyznonexistent", null, null, null, null, null);
            Page<Task> result = taskRepository.findAll(spec, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }
}
