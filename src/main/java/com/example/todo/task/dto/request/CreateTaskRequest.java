package com.example.todo.task.dto.request;
import com.example.todo.task.entity.TaskPriority;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CreateTaskRequest {
    @NotBlank(message = "Title is required") @Size(min = 3, max = 100)
    private String title;
    @NotNull(message = "Priority is required")
    private TaskPriority priority;
    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;
    @NotNull(message = "User ID is required")
    private Long userId;

    public CreateTaskRequest() {}

    public CreateTaskRequest(String title, TaskPriority priority, LocalDate dueDate, Long userId) {
        this.title = title;
        this.priority = priority;
        this.dueDate = dueDate;
        this.userId = userId;
    }

    // Getters and Setters...
    public String getTitle() { return title; }
    public TaskPriority getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public Long getUserId() { return userId; }
    public void setTitle(String title) { this.title = title; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setUserId(Long userId) { this.userId = userId; }
}