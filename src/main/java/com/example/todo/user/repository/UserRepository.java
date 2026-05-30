package com.example.todo.user.repository;

import com.example.todo.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Needed by Spring Security to load user during login
    Optional<AppUser> findByUsername(String username);
}
