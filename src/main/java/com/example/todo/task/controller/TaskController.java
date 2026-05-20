package com.example.todo.task.controller;

import com.example.todo.task.dto.request.*;
import com.example.todo.task.dto.response.*;
import com.example.todo.task.entity.Task;
import com.example.todo.task.entity.TaskPriority;
import com.example.todo.task.mapper.TaskMapper;
import com.example.todo.task.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    public ResponseEntity<TaskPageResponse> getAllTasks(
            @RequestParam(required = false) Boolean completed,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,

            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 50, message = "Size must not be greater than 50")
            int size,

            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Page<Task> taskPage = taskService.getAllTasks(
                completed,
                page,
                size,
                sortBy,
                direction
        );

        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }

    @GetMapping("/search")
    public ResponseEntity<TaskPageResponse> searchTasks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) TaskPriority priority,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueAfter,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueBefore,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,

            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 50, message = "Size must not be greater than 50")
            int size,

            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Page<Task> taskPage = taskService.searchTasks(
                title,
                completed,
                priority,
                dueAfter,
                dueBefore,
                dueDate,
                page,
                size,
                sortBy,
                direction
        );

        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request
    ) {
        Task createdTask = taskService.createTask(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(taskMapper.toResponse(createdTask));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        Task updatedTask = taskService.updateTask(id, request);
        return ResponseEntity.ok(taskMapper.toResponse(updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> patchTask(@PathVariable Long id, @Valid @RequestBody PatchTaskRequest request){
        Task patchedTask = taskService.patchTask(id, request);
        return ResponseEntity.ok(taskMapper.toResponse(patchedTask));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(@PathVariable Long id) {
        Task task = taskService.completeTask(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TaskResponse> reopenTask(@PathVariable Long id) {
        Task task = taskService.reopenTask(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }



}
