package com.example.todo.comment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
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