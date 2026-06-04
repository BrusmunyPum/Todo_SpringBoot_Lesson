package com.example.todo.auth.controller;

import com.example.todo.auth.dto.request.LoginRequest;
import com.example.todo.auth.dto.request.RegisterRequest;
import com.example.todo.auth.dto.response.LoginResponse;
import com.example.todo.auth.dto.response.RegisterResponse;
import com.example.todo.auth.service.AuthService;
import com.example.todo.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Register, login, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new account")
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
    @Operation(summary = "Login and receive a JWT token",
               description = "Copy the `accessToken` from the response, click **Authorize** at the top of this page, and paste it in.")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (client-side)",
               description = "JWT is stateless — the server holds no session. Logout by deleting the token on the client.")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
