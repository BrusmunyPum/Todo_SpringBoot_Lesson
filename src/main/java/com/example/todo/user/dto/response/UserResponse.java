package com.example.todo.user.dto.response;
import java.time.Instant;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;

    public UserResponse(Long id, String username, String email, Instant createdAt, Instant updatedAt) {
        this.id = id; this.username = username; this.email = email; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}