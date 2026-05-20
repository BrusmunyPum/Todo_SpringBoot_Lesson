package com.example.todo.comment.repository;

import com.example.todo.comment.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
