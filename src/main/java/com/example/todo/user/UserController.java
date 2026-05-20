package com.example.todo.user;


import com.example.todo.task.Task;
import com.example.todo.task.TaskMapper;
import com.example.todo.task.TaskPageResponse;
import com.example.todo.task.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
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

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){
        AppUser user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id){
        AppUser user = userService.getUserById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<TaskPageResponse> getTasksByUserId(
            @PathVariable Long id,

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
        Page<Task> taskPage = taskService.getTasksByUserId(
                id,
                page,
                size,
                sortBy,
                direction
        );

        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<UserDetailResponse> getUserDetailById(@PathVariable Long id){
        return ResponseEntity.ok(userService.getUserDetail(id));
    }


    @GetMapping("/{id}/tasks/completed")
    public ResponseEntity<TaskPageResponse> getCompletedTasksByUserId(
            @PathVariable Long id,

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
        Page<Task> taskPage = taskService.getCompletedTasksByUserId(
                id,
                page,
                size,
                sortBy,
                direction
        );

        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }

}
