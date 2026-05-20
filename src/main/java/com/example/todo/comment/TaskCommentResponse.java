package com.example.todo.comment;

import java.time.Instant;

public class TaskCommentResponse {
    private Long id;
    private Long taskId;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public TaskCommentResponse(
            Long id,
            Long taskId,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.taskId = taskId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}