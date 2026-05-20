package com.example.todo.task.dto.request;

import com.example.todo.task.entity.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PatchTaskRequest {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;
    private Boolean completed;
    private TaskPriority priority;

    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public TaskPriority getPriority() {
        return priority;
    }
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    public LocalDate getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
