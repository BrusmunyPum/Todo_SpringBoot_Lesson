//package com.example.todo.task;
//
//import com.example.todo.common.exception.TaskNotFoundException;
//import org.springframework.stereotype.Service;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.transaction.annotation.Transactional;
//
//
//import java.time.LocalDate;
//import java.util.List;
//
//@Service
//@Transactional(readOnly = true)
//public class TaskService {

    // before database
//    private final List<Task> tasks = new ArrayList<>();
//    private  Long nextId = 1L;
//
//    public List<Task> getAllTasks(){
//        return tasks;
//    }
//
//    public Task getTaskById(Long id){
//        // return tasks.stream().filter(task -> task.getId().equals(id)).findFirst().orElse(null);
//        return tasks.stream().filter(task -> task.getId().equals(id)).findFirst().orElseThrow(() -> new TaskNotFoundException(id));
//    }
//
//    public Task createTask(CreateTaskRequest request){
//        Task task = new Task(nextId, request.getTitle(), false);
//        tasks.add(task);
//        nextId++;
//        return task;
//    }
//
//    public Task updateTask(Long id, UpdateTaskRequest request){
//        Task task = getTaskById(id);
//
//        if (task == null){
//            return null;
//        }
//        task.setTitle(request.getTitle());
//        task.setCompleted(request.isCompleted());
//
//        return task;
//    }
//
//    public void deleteTask(Long id){
//        boolean removed = tasks.removeIf(task -> task.getId().equals(id));
//
//        if (!removed){
//            throw new TaskNotFoundException(id);
//        }
//    }

//    // After database
//
//    private final TaskRepository taskRepository;
//
//    public TaskService(TaskRepository taskRepository) {
//        this.taskRepository = taskRepository;
//    }

//    public List<Task> getAllTasks(){
//        return taskRepository.findAll();
//    }
//
//    public Task getTaskById(Long id){
//        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
//    }
//
//    public Task createTask(CreateTaskRequest request){
//        Task task = new Task(request.getTitle(),false);
//        return taskRepository.save(task);
//    }
//
//    public Task updateTask(Long id, UpdateTaskRequest request){
//        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
//
//        task.setTitle(request.getTitle());
//        task.setCompleted(request.isCompleted());
//
//        return taskRepository.save(task);
//    }
//
//    public void deleteTask(Long id){
//        Task task = getTaskById(id);
//        taskRepository.delete(task);
//    }

//    public List<Task> getAllTasks(Boolean completed) {
//        if (completed == null) {
//            return taskRepository.findAll();
//        }
//
//        return taskRepository.findByCompleted(completed);
//    }

//    public Page<Task> getAllTasks(Boolean completed, int page, int size, String sortBy, String direction){
//        Sort sort = direction.equalsIgnoreCase("desc")
//                ? Sort.by(sortBy).descending()
//                : Sort.by(sortBy).ascending();
//
//        Pageable pageable = PageRequest.of(page, size, sort);
//
//        if(completed == null){
//            return taskRepository.findAll(pageable);
//        }
//
//        return taskRepository.findByCompleted(completed, pageable);
//    }

//    private String validateSortBy(String sortBy) {
//        List<String> allowedSortFields = List.of("id", "title", "completed");
//
//        if (!allowedSortFields.contains(sortBy)) {
//            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
//        }
//
//        return sortBy;
//    }

//    private String validateSortBy(String sortBy) {
//        List<String> allowedSortFields = List.of(
//                "id",
//                "title",
//                "completed",
//                "priority",
//                "dueDate",
//                "createdAt",
//                "updatedAt"
//        );
//
//        if (!allowedSortFields.contains(sortBy)) {
//            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
//        }
//
//        return sortBy;
//    }
//
//    private Sort.Direction validateDirection(String direction) {
//        if (direction.equalsIgnoreCase("asc")) {
//            return Sort.Direction.ASC;
//        }
//
//        if (direction.equalsIgnoreCase("desc")) {
//            return Sort.Direction.DESC;
//        }
//
//        throw new IllegalArgumentException("Invalid sort direction: " + direction);
//    }
//
//    public Page<Task> getAllTasks(
//            Boolean completed,
//            int page,
//            int size,
//            String sortBy,
//            String direction
//    ) {
//        String safeSortBy = validateSortBy(sortBy);
//        Sort.Direction safeDirection = validateDirection(direction);
//
//        Sort sort = Sort.by(safeDirection, safeSortBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//
//        if (completed == null) {
//            return taskRepository.findAll(pageable);
//        }
//
//        return taskRepository.findByCompleted(completed, pageable);
//    }
//
//
//    public List<Task> searchTasksByTitle(String title) {
//        return taskRepository.findByTitleContainingIgnoreCase(title);
//    }
//
//    public Task getTaskById(Long id) {
//        return taskRepository.findById(id)
//                .orElseThrow(() -> new TaskNotFoundException(id));
//    }
//
//    public Task createTask(CreateTaskRequest request) {
//        Task task = new Task(request.getTitle(), false, request.getPriority(), request.getDueDate());
//        return taskRepository.save(task);
//    }
//
//    public Task updateTask(Long id, UpdateTaskRequest request) {
//        Task task = getTaskById(id);
//
//        task.setTitle(request.getTitle());
//        task.setCompleted(request.isCompleted());
//        task.setPriority(request.getPriority());
//        task.setDueDate(request.getDueDate());
//
//        return taskRepository.save(task);
//    }
//
//    public void deleteTask(Long id) {
//        Task task = getTaskById(id);
//        taskRepository.delete(task);
//    }
//
//    public Task patchTask(Long id, PatchTaskRequest request) {
//        Task task = getTaskById(id);
//
//        if(request.getTitle() != null) {
//            task.setTitle(request.getTitle());
//        }
//
//        if(request.getCompleted() != null) {
//            task.setCompleted(request.getCompleted());
//        }
//
//        if(request.getPriority() != null) {
//            task.setPriority(request.getPriority());
//        }
//
//        if(request.getDueDate() != null) {
//            task.setDueDate(request.getDueDate());
//        }
//
//        return taskRepository.save(task);
//    }
//
////    public Task completeTask(Long id, PatchTaskRequest request) {
////        Task task = getTaskById(id);
////        if(request.getCompleted() != null) {
////            task.setCompleted(request.getCompleted());
////        }
////        return taskRepository.save(task);
////    }
//
//    public Task completeTask(Long id) {
//        Task task = getTaskById(id);
//        task.setCompleted(true);
//        return taskRepository.save(task);
//    }
//
//    public Page<Task> searchTasks(
//            String title,
//            Boolean completed,
//            TaskPriority priority,
//            LocalDate dueAfter,
//            LocalDate dueBefore,
//            LocalDate dueDate,
//            int page,
//            int size,
//            String sortBy,
//            String direction
//    ) {
//        String safeSortBy = validateSortBy(sortBy);
//        Sort.Direction safeDirection = validateDirection(direction);
//
//        Sort sort = Sort.by(safeDirection, safeSortBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//
//        return taskRepository.findAll(
//                TaskSpecifications.withFilters(
//                        title,
//                        completed,
//                        priority,
//                        dueAfter,
//                        dueBefore,
//                        dueDate
//                ),
//                pageable
//        );
//    }
//}

package com.example.todo.task;

import com.example.todo.common.exception.TaskAlreadyCompletedException;
import com.example.todo.common.exception.TaskNotFoundException;
import com.example.todo.user.AppUser;
import com.example.todo.user.UserRepository;
import com.example.todo.user.UserService;
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

//    @Transactional
//    public Task completeTask(Long id) {
//        Task task = getTaskById(id);
//        task.setCompleted(true);
//        return taskRepository.save(task);
//    }

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