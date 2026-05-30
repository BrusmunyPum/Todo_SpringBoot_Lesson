package com.example.todo.auth.dto.response;

public class RegisterResponse {

    private final Long id;
    private final String username;
    private final String email;

    public RegisterResponse(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
