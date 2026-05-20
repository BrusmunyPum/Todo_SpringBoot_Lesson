package com.example.todo.auth.controller;

import com.example.todo.auth.dto.request.RegisterRequest;
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
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        AppUser newUser = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully with ID: " + newUser.getId());
    }
}