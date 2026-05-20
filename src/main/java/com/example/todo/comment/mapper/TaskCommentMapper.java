package com.example.todo.comment.mapper;

import com.example.todo.comment.dto.response.TaskCommentResponse;
import com.example.todo.comment.entity.TaskComment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskCommentMapper {

    public TaskCommentResponse toResponse(TaskComment comment) {
        return new TaskCommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public List<TaskCommentResponse> toResponseList(List<TaskComment> comments) {
        return comments.stream()
                .map(this::toResponse)
                .toList();
    }
}