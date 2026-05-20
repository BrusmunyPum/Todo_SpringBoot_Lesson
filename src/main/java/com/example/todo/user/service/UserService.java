package com.example.todo.user.service;

import com.example.todo.common.exception.UserNotFoundException;
import com.example.todo.task.repository.TaskRepository;
import com.example.todo.user.dto.response.UserDetailResponse;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    public AppUser getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public UserDetailResponse getUserDetail(Long id) {
        AppUser user = getUserById(id);
        long taskCount = taskRepository.countByUserId(id);

        return new UserDetailResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                taskCount, user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}