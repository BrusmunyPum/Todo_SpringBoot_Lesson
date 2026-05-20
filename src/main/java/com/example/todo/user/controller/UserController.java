package com.example.todo.user.controller;

import com.example.todo.task.dto.response.TaskPageResponse;
import com.example.todo.task.entity.Task;
import com.example.todo.task.mapper.TaskMapper;
import com.example.todo.task.service.TaskService;
import com.example.todo.user.dto.response.UserDetailResponse;
import com.example.todo.user.dto.response.UserResponse;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.mapper.UserMapper;
import com.example.todo.user.service.UserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public UserController(UserService userService, UserMapper userMapper, TaskService taskService, TaskMapper taskMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return ResponseEntity.ok(userMapper.toResponse(userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id){
        AppUser user = userService.getUserById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<UserDetailResponse> getUserDetailById(@PathVariable Long id){
        return ResponseEntity.ok(userService.getUserDetail(id));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<TaskPageResponse> getTasksByUserId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Page<Task> taskPage = taskService.getTasksByUserId(id, page, size, sortBy, direction);
        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }

    @GetMapping("/{id}/tasks/completed")
    public ResponseEntity<TaskPageResponse> getCompletedTasksByUserId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Page<Task> taskPage = taskService.getCompletedTasksByUserId(id, page, size, sortBy, direction);
        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }
}