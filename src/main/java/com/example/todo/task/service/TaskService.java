package com.example.todo.task.service;

import com.example.todo.common.exception.TaskAlreadyCompletedException;
import com.example.todo.common.exception.TaskNotFoundException;
import com.example.todo.task.dto.request.CreateTaskRequest;
import com.example.todo.task.dto.request.PatchTaskRequest;
import com.example.todo.task.dto.request.UpdateTaskRequest;
import com.example.todo.task.entity.Task;
import com.example.todo.task.entity.TaskPriority;
import com.example.todo.task.repository.TaskRepository;
import com.example.todo.task.repository.TaskSpecifications;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    public Page<Task> getAllTasks(
            Boolean completed,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        String safeSortBy = validateSortBy(sortBy);
        Sort.Direction safeDirection = validateDirection(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(safeDirection, safeSortBy)
        );

        if (completed == null) {
            return taskRepository.findAll(pageable);
        }

        return taskRepository.findByCompleted(completed, pageable);
    }

    public Page<Task> searchTasks(
            String title,
            Boolean completed,
            TaskPriority priority,
            LocalDate dueAfter,
            LocalDate dueBefore,
            LocalDate dueDate,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        String safeSortBy = validateSortBy(sortBy);
        Sort.Direction safeDirection = validateDirection(direction);

        Sort sort = Sort.by(safeDirection, safeSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return taskRepository.findAll(
                TaskSpecifications.withFilters(
                        title,
                        completed,
                        priority,
                        dueAfter,
                        dueBefore,
                        dueDate
                ),
                pageable
        );
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public Task createTask(CreateTaskRequest request) {
        AppUser user = userService.getUserById(request.getUserId());

        Task task = new Task(
                request.getTitle(),
                false,
                request.getPriority(),
                request.getDueDate(),
                user
        );

        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskById(id);

        task.setTitle(request.getTitle());
        task.setCompleted(request.isCompleted());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        return taskRepository.save(task);
    }

    @Transactional
    public Task patchTask(Long id, PatchTaskRequest request) {
        Task task = getTaskById(id);

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }

        if (request.getCompleted() != null) {
            task.setCompleted(request.getCompleted());
        }

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        return taskRepository.save(task);
    }

    @Transactional
    public Task completeTask(Long id) {
        Task task = getTaskById(id);

        if (task.isCompleted()) {
            throw new TaskAlreadyCompletedException(id);
        }

        task.setCompleted(true);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    private String validateSortBy(String sortBy) {
        List<String> allowedSortFields = List.of(
                "id",
                "title",
                "completed",
                "priority",
                "dueDate",
                "createdAt",
                "updatedAt"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }

        return sortBy;
    }

    private Sort.Direction validateDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        }

        if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }

        throw new IllegalArgumentException("Invalid sort direction: " + direction);
    }

    @Transactional
    public Task reopenTask(Long id) {
        Task task = getTaskById(id);
        task.setCompleted(false);
        return taskRepository.save(task);
    }

    public Page<Task> getTasksByUserId(
            Long userId,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        userService.getUserById(userId);

        String safeSortBy = validateSortBy(sortBy);
        Sort.Direction safeDirection = validateDirection(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(safeDirection, safeSortBy)
        );

        return taskRepository.findByUserId(userId, pageable);
    }

    public Page<Task> getCompletedTasksByUserId(
            Long userId,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        userService.getUserById(userId);

        String safeSortBy = validateSortBy(sortBy);
        Sort.Direction safeDirection = validateDirection(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(safeDirection, safeSortBy)
        );

        return taskRepository.findByUserIdAndCompleted(userId, true, pageable);
    }

}