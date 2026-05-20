package com.example.todo.user.repository;

import com.example.todo.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}