package com.example.todo.auth.dto.response;

public class LoginResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final long expiresIn; // seconds

    public LoginResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
}
