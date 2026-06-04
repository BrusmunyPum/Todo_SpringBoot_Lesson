package com.example.todo.comment.controller;

import com.example.todo.comment.dto.request.CreateTaskCommentRequest;
import com.example.todo.comment.dto.response.TaskCommentResponse;
import com.example.todo.comment.entity.TaskComment;
import com.example.todo.comment.mapper.TaskCommentMapper;
import com.example.todo.comment.service.TaskCommentService;
import com.example.todo.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@Tag(name = "Comments", description = "Manage comments on tasks. Only the task owner (or admin) can create, edit, or delete comments.")
@SecurityRequirement(name = "bearerAuth")
public class TaskCommentController {

    private final TaskCommentService taskCommentService;
    private final TaskCommentMapper taskCommentMapper;

    public TaskCommentController(
            TaskCommentService taskCommentService,
            TaskCommentMapper taskCommentMapper
    ) {
        this.taskCommentService = taskCommentService;
        this.taskCommentMapper = taskCommentMapper;
    }

    @GetMapping
    @Operation(summary = "List all comments for a task")
    public ResponseEntity<List<TaskCommentResponse>> getCommentsByTaskId(
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(
                taskCommentMapper.toResponseList(
                        taskCommentService.getCommentsByTaskId(taskId)
                )
        );
    }

    @PostMapping
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<TaskCommentResponse> createComment(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody CreateTaskCommentRequest request
    ) {
        TaskComment comment = taskCommentService.createComment(taskId, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskCommentMapper.toResponse(comment));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Update a comment")
    public ResponseEntity<TaskCommentResponse> updateComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody CreateTaskCommentRequest request
    ) {
        TaskComment comment = taskCommentService.updateComment(taskId, commentId, currentUser, request);
        return ResponseEntity.ok(taskCommentMapper.toResponse(comment));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        taskCommentService.deleteComment(taskId, commentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
