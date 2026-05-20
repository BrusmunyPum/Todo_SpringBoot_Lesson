package com.example.todo.auth.service;

import com.example.todo.auth.dto.request.RegisterRequest;
import com.example.todo.common.exception.DuplicateResourceException;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}