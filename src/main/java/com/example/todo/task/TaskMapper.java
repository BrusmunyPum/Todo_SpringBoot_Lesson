package com.example.todo.task;


import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskMapper {
    public TaskResponse toResponse(Task task) {
        Long userId = task.getUser() != null ? task.getUser().getId() : null;
        String username = task.getUser() != null ? task.getUser().getUsername() : null;


        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.isCompleted(),
                task.getPriority(),
                task.getDueDate(),
                userId,
                username,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public List<TaskResponse> toResponseList(List<Task> tasks) {
        return tasks.stream().map(this::toResponse).toList();
    }

    public TaskPageResponse toPageResponse(Page<Task> taskPage) {
        List<TaskResponse> content = taskPage.getContent().stream().map(this::toResponse).toList();
        return new TaskPageResponse(
                content,
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.isFirst(),
                taskPage.isLast()
        );
    }
}
