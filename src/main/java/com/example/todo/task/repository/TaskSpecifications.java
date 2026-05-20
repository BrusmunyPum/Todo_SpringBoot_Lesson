package com.example.todo.task.repository;

import com.example.todo.task.entity.Task;
import com.example.todo.task.entity.TaskPriority;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> withFilters(
            String title,
            Boolean completed,
            TaskPriority priority,
            LocalDate dueAfter,
            LocalDate dueBefore,
            LocalDate dueDate
    ) {
        return Specification
                .where(titleContains(title))
                .and(hasCompleted(completed))
                .and(hasPriority(priority))
                .and(dueDateAfterOrEqual(dueAfter))
                .and(dueDateBeforeOrEqual(dueBefore))
                .and(hasDueDate(dueDate));
    }

    private static Specification<Task> titleContains(String title) {
        return (root, query, criteriaBuilder) -> {
            if (title == null || title.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    "%" + title.toLowerCase() + "%"
            );
        };
    }

    private static Specification<Task> hasCompleted(Boolean completed) {
        return (root, query, criteriaBuilder) -> {
            if (completed == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("completed"), completed);
        };
    }

    private static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, criteriaBuilder) -> {
            if (priority == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("priority"), priority);
        };
    }

    private static Specification<Task> dueDateAfterOrEqual(LocalDate dueAfter) {
        return (root, query, criteriaBuilder) -> {
            if (dueAfter == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueAfter);
        };
    }

    private static Specification<Task> dueDateBeforeOrEqual(LocalDate dueBefore) {
        return (root, query, criteriaBuilder) -> {
            if (dueBefore == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueBefore);
        };
    }

    private static Specification<Task> hasDueDate(LocalDate dueDate) {
        return (root, query, criteriaBuilder) -> {
            if (dueDate == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("dueDate"), dueDate);
        };
    }
}