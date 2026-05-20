package com.example.todo.task;

import java.time.Instant;
import java.time.LocalDate;

public class TaskResponse {
    private Long id;
    private String title;
    private boolean completed;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    // App user
    private Long userId;
    private String userName;

    public TaskResponse(
            Long id,
            String title,
            boolean completed,
            TaskPriority priority,
            LocalDate dueDate,
            Long userId,
            String userName,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.priority = priority;
        this.dueDate = dueDate;
        this.userId = userId;
        this.userName = userName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

}