package com.example.todo.comment.controller;

import com.example.todo.comment.dto.request.CreateTaskCommentRequest;
import com.example.todo.comment.dto.response.TaskCommentResponse;
import com.example.todo.comment.entity.TaskComment;
import com.example.todo.comment.mapper.TaskCommentMapper;
import com.example.todo.comment.service.TaskCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
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
    public ResponseEntity<TaskCommentResponse> createComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateTaskCommentRequest request
    ) {
        TaskComment comment = taskCommentService.createComment(taskId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(taskCommentMapper.toResponse(comment));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId
    ) {
        taskCommentService.deleteComment(taskId, commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<TaskCommentResponse> updateComment(@PathVariable Long taskId, @PathVariable Long commentId, @Valid @RequestBody CreateTaskCommentRequest request){
        TaskComment comment = taskCommentService.updateComment(taskId, commentId, request);
        return ResponseEntity.ok(taskCommentMapper.toResponse(comment));
    }
}