package com.example.todo.task.repository;

import com.example.todo.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // Override findById to eagerly load user — prevents LazyInitializationException
    // when TaskMapper accesses task.getUser() after the Hibernate session closes.
    // Without this, findById returns a Task with a lazy user proxy that can't be
    // initialized outside of a transaction (open-in-view is disabled).
    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = "user")
    Page<Task> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByCompleted(boolean completed, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByUserIdAndCompleted(Long userId, boolean completed, Pageable pageable);

    Long countByUserId(Long userId);
    List<Task> findByTitleContainingIgnoreCase(String title);
}