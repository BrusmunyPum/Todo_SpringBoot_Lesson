//package com.example.todo.user;
//
//import com.example.todo.task.TaskRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@Transactional(readOnly = true)
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final TaskRepository taskRepository;
//
//    public UserService(UserRepository userRepository, TaskRepository taskRepository) {
//        this.userRepository = userRepository;
//        this.taskRepository = taskRepository;
//    }
//
//    public List<AppUser> getAllUsers() {
//        return userRepository.findAll();
//    }
//
//    public AppUser getUserById(Long id) {
//        return userRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
//    }
//
//    public UserDetailResponse getUserDetail(Long id) {
//        AppUser user = getUserById(id);
//        Long taskCount = taskRepository.countByUserId(id);
//
//        return new UserDetailResponse(
//                user.getId(),
//                user.getUsername(),
//                user.getEmail(),
//                taskCount,
//                user.getCreatedAt(),
//                user.getUpdatedAt()
//        );
//
//    }
//
//
//    @Transactional
//    public AppUser createUser(CreateUserRequest request) {
//        if (userRepository.existsByUsername(request.getUsername())) {
//            throw new IllegalArgumentException("Username already exists");
//        }
//
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new IllegalArgumentException("Email already exists");
//        }
//
//        AppUser user = new AppUser(
//                request.getUsername(),
//                request.getEmail()
//        );
//
//        return userRepository.save(user);
//    }
//}

package com.example.todo.user;

import com.example.todo.common.exception.DuplicateResourceException;
import com.example.todo.common.exception.UserNotFoundException;
import com.example.todo.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public UserService(
            UserRepository userRepository,
            TaskRepository taskRepository
    ) {
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
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                taskCount,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public AppUser createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        AppUser user = new AppUser(
                request.getUsername(),
                request.getEmail(),
                request.ge
        );

        return userRepository.save(user);
    }
}