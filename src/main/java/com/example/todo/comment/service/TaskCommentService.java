package com.example.todo.comment.service;

import com.example.todo.comment.dto.request.CreateTaskCommentRequest;
import com.example.todo.comment.entity.TaskComment;
import com.example.todo.comment.repository.TaskCommentRepository;
import com.example.todo.common.exception.BadRequestException;
import com.example.todo.common.exception.CommentNotFoundException;
import com.example.todo.task.entity.Task;
import com.example.todo.task.service.TaskService;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.entity.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskService taskService;

    public TaskCommentService(
            TaskCommentRepository taskCommentRepository,
            TaskService taskService
    ) {
        this.taskCommentRepository = taskCommentRepository;
        this.taskService = taskService;
    }

    public List<TaskComment> getCommentsByTaskId(Long taskId) {
        taskService.getTaskById(taskId); // verify task exists
        return taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Transactional
    public TaskComment createComment(Long taskId, AppUser currentUser, CreateTaskCommentRequest request) {
        Task task = taskService.getTaskById(taskId);
        checkTaskOwnership(task, currentUser);

        TaskComment comment = new TaskComment(request.getContent(), task);
        return taskCommentRepository.save(comment);
    }

    @Transactional
    public TaskComment updateComment(
            Long taskId,
            Long commentId,
            AppUser currentUser,
            CreateTaskCommentRequest request
    ) {
        Task task = taskService.getTaskById(taskId);
        checkTaskOwnership(task, currentUser);

        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getTask().getId().equals(taskId)) {
            throw new BadRequestException("Comment does not belong to task id: " + taskId);
        }

        comment.setContent(request.getContent());
        return taskCommentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long taskId, Long commentId, AppUser currentUser) {
        Task task = taskService.getTaskById(taskId);
        checkTaskOwnership(task, currentUser);

        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getTask().getId().equals(taskId)) {
            throw new BadRequestException("Comment does not belong to task id: " + taskId);
        }

        taskCommentRepository.delete(comment);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Admins can manage comments on any task.
     * Regular users can only manage comments on their own tasks.
     */
    private void checkTaskOwnership(Task task, AppUser currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) return;
        if (!task.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to access this task's comments");
        }
    }
}
