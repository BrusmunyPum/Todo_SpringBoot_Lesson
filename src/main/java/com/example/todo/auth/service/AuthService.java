package com.example.todo.auth.service;

import com.example.todo.auth.dto.request.LoginRequest;
import com.example.todo.auth.dto.request.RegisterRequest;
import com.example.todo.auth.dto.response.LoginResponse;
import com.example.todo.common.exception.DuplicateResourceException;
import com.example.todo.common.exception.InvalidCredentialsException;
import com.example.todo.common.security.JwtService;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        AppUser newUser = new AppUser(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        return userRepository.save(newUser);
    }

    public LoginResponse login(LoginRequest request) {
        // Step 1: Find user by username
        // If not found → throw InvalidCredentialsException (plain RuntimeException)
        // We do NOT say "user not found" — always say "invalid credentials" for security
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Step 2: Verify password using BCrypt
        // passwordEncoder.matches(rawPassword, hashedPassword)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Step 3: Generate JWT token
        String token = jwtService.generateToken(user);

        return new LoginResponse(token, jwtExpiration / 1000);
    }
}
