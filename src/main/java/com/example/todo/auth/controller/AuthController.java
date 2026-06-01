package com.example.todo.auth.controller;

import com.example.todo.auth.dto.request.LoginRequest;
import com.example.todo.auth.dto.request.RegisterRequest;
import com.example.todo.auth.dto.response.LoginResponse;
import com.example.todo.auth.dto.response.RegisterResponse;
import com.example.todo.auth.service.AuthService;
import com.example.todo.user.entity.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AppUser newUser = authService.register(request);
        RegisterResponse response = new RegisterResponse(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless — the server holds no session to destroy.
        // The real logout happens on the client: delete the token from
        // localStorage / memory. This endpoint exists so the frontend
        // has a clean API to call, and so we can add server-side logic
        // later (e.g. token blacklist) without changing the frontend.
        return ResponseEntity.noContent().build();
    }
}