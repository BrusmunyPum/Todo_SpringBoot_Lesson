package com.example.todo.auth.service;

import com.example.todo.auth.dto.request.LoginRequest;
import com.example.todo.auth.dto.request.RegisterRequest;
import com.example.todo.auth.dto.response.LoginResponse;
import com.example.todo.common.exception.DuplicateResourceException;
import com.example.todo.common.security.JwtService;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        AppUser newUser = new AppUser(
                request.getUsername(),
                request.getEmail(),
                hashedPassword
        );

        return userRepository.save(newUser);
    }

    public LoginResponse login(LoginRequest request) {
        // 1. Let Spring Security verify username + password.
        //    If wrong, it throws BadCredentialsException automatically.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 2. Credentials are correct — load the user
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        // 3. Generate JWT token
        String token = jwtService.generateToken(user);

        // 4. Return token with expiry in seconds
        return new LoginResponse(token, jwtExpiration / 1000);
    }
}
