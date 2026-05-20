//package com.example.todo.task;
//
//
//import org.springframework.data.domain.Page;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import jakarta.validation.Valid;
//
//import jakarta.validation.constraints.Max;
//import jakarta.validation.constraints.Min;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@RestController
//@RequestMapping("api/tasks")
//public class TaskController {
//
//    // First step learn
////    private final List<Task> tasks = new ArrayList<>();
////    private Long nextId = 1L;
////
////    @GetMapping
////    public List<Task> getAllTasks(){
////        return tasks;
////    }
////
////    @GetMapping("/{id}")
////    public Task getTaskById(@PathVariable Long id){
////        return tasks.stream()
////                .filter(task -> task.getId().equals(id))
////                .findFirst()
////                .orElse(null);
////    }
////
////    @PostMapping
////    public Task createTask(@RequestBody CreateTaskRequest request){
////        Task task = new Task(nextId, request.getTitle(), false);
////        tasks.add(task);
////        nextId++;
////        return task;
////    }
////
////    @PutMapping("/{id}")
////    public Task updateTask(@PathVariable Long id, @RequestBody UpdateTaskRequest request){
////        Task task = tasks.stream().filter(existTask -> existTask.getId().equals(id)).findFirst().orElse(null);
////
////        if (task == null){
////            return null;
////        }
////
////        task.setTitle(request.getTitle());
////        task.setCompleted(request.isCompleted());
////
////        return task;
////    }
////
////    @DeleteMapping("/{id}")
////    public String deleteTask(@PathVariable Long id){
////        boolean removed = tasks.removeIf(task -> task.getId().equals(id));
////
////        if (removed){
////            return "Task has been deleted";
////        }
////
////        return "Task has been deleted";
////    }
//
//    // Practice service logic step one
//
////    public final TaskService taskService;
////
////    public TaskController(TaskService taskService){
////        this.taskService = taskService;
////    }
////
////    @GetMapping
////    public List<Task> getAllTasks(){
////        return taskService.getAllTasks();
////    }
////
////    @GetMapping("/{id}")
////    public Task getTaskById(@PathVariable Long id){
////        return taskService.getTaskById(id);
////    }
////
////    @PostMapping
////    public Task createTask(@RequestBody CreateTaskRequest request){
////        return taskService.createTask(request);
////    }
////
////    @PutMapping("/{id}")
////    public Task UpdateTask(@PathVariable Long id, @RequestBody UpdateTaskRequest request){
////        return taskService.updateTask(id,request);
////    }
////
////    @DeleteMapping("/{id}")
////    public String deleteTask(@PathVariable Long id){
////        boolean removed = taskService.deleteTask(id);
////
////        if (removed){
////            return "Task has been deleted";
////        }
////
////        return "Task not found";
////    }
//
//
//    // Apply ResponseEntity
//
////    public final TaskService taskService;
////
////    public TaskController(TaskService taskService){
////        this.taskService = taskService;
////    }
////
////    @GetMapping
////    public ResponseEntity<List<Task>> getAllTasks(){
////        List<Task> tasks = taskService.getAllTasks();
////        return ResponseEntity.ok(tasks);
////    }
////
////    @GetMapping("/{id}")
////    public ResponseEntity<Task> getTaskById(@PathVariable Long id){
////        Task task = taskService.getTaskById(id);
////
////        if(task == null){
////            return ResponseEntity.notFound().build();
////        }
////
////        return ResponseEntity.ok(task);
////    }
////
////    @PostMapping
////    public ResponseEntity<Task> createTask(@Valid @RequestBody CreateTaskRequest request){
////        Task createtask = taskService.createTask(request);
////
////        return ResponseEntity.status(HttpStatus.CREATED).body(createtask);
////    }
////
////    @PutMapping("/{id}")
////    public ResponseEntity<Task> updateTask(@PathVariable Long id,@Valid @RequestBody UpdateTaskRequest request){
////        Task updatetask = taskService.updateTask(id,request);
////
////        if(updatetask == null){
////            return ResponseEntity.notFound().build();
////        }
////
////        return ResponseEntity.ok(updatetask);
////    }
////
////    @DeleteMapping("/{id}")
////    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
////        boolean removed = taskService.deleteTask(id);
////        if(removed){
////            return ResponseEntity.noContent().build();
////        }
////
////        return ResponseEntity.notFound().build();
////    }
//
//    // Apply Exception
//
//    private final TaskService taskService;
//
//    public TaskController(TaskService taskService) {
//        this.taskService = taskService;
//    }
//
//    private TaskResponse toResponse(Task task) {
//        return new TaskResponse(
//                task.getId(),
//                task.getTitle(),
//                task.isCompleted(),
//                task.getPriority(),
//                task.getDueDate(),
//                task.getCreatedAt(),
//                task.getUpdatedAt()
//        );
//    }
//
//    @GetMapping
////    public ResponseEntity<List<Task>> getAllTasks() {
////        return ResponseEntity.ok(taskService.getAllTasks());
////    }
////    public ResponseEntity<List<TaskResponse>> getAllTasks(){
////        List<TaskResponse> response = taskService.getAllTasks().stream().map(this::toResponse).toList();
////        return ResponseEntity.ok(response);
////    }
//
////    public ResponseEntity<List<TaskResponse>> getAllTasks(
////            @RequestParam(required = false) Boolean completed
////    ) {
////        List<TaskResponse> response = taskService.getAllTasks(completed)
////                .stream()
////                .map(this::toResponse)
////                .toList();
////
////        return ResponseEntity.ok(response);
////    }
////
////    @GetMapping("/search")
////    public ResponseEntity<List<TaskResponse>> searchTasks(
////            @RequestParam String title
////    ) {
////        List<TaskResponse> response = taskService.searchTasksByTitle(title)
////                .stream()
////                .map(this::toResponse)
////                .toList();
////
////        return ResponseEntity.ok(response);
////    }
//
//    public ResponseEntity<TaskPageResponse> getAllTasks(@RequestParam(required = false) Boolean completed,
//                                                        @RequestParam(defaultValue = "0")
//                                                        @Min(value = 0, message = "Size must be at least 1")
//                                                        int page,
//                                                        @RequestParam(defaultValue = "5")
//                                                            @Min(value = 1, message = "Size must be at least 1")
//                                                            @Max(value = 50, message = "Size must not be greater then 50")
//                                                            int size,
//                                                        @RequestParam(defaultValue = "id") String sortBy,
//                                                        @RequestParam(defaultValue = "asc") String direction
//                                                        ){
//        Page<Task> taskPage = taskService.getAllTasks(completed, page, size, sortBy, direction);
//
//        List<TaskResponse> content = taskPage.getContent().stream().map(this::toResponse).toList();
//
//        TaskPageResponse response = new TaskPageResponse(content,
//                taskPage.getNumber(),
//                taskPage.getSize(),
//                taskPage.getTotalElements(),
//                taskPage.getTotalPages(),
//                taskPage.isFirst(),
//                taskPage.isLast()
//        );
//
//        return ResponseEntity.ok(response);
//    }
//
//
//    @GetMapping("/{id}")
////    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
////        return ResponseEntity.ok(taskService.getTaskById(id));
////    }
//
//    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
//        Task task = taskService.getTaskById(id);
//        return ResponseEntity.ok(toResponse(task));
//    }
//
//    @PostMapping
////    public ResponseEntity<Task> createTask(@Valid @RequestBody CreateTaskRequest request) {
////        Task createdTask = taskService.createTask(request);
////
////        return ResponseEntity
////                .status(HttpStatus.CREATED)
////                .body(createdTask);
////    }
//
//    public ResponseEntity<TaskResponse> createTask(
//            @Valid @RequestBody CreateTaskRequest request
//    ) {
//        Task createdTask = taskService.createTask(request);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(toResponse(createdTask));
//    }
//
//    @PutMapping("/{id}")
////    public ResponseEntity<Task> updateTask(
////            @PathVariable Long id,
////            @Valid @RequestBody UpdateTaskRequest request
////    ) {
////        return ResponseEntity.ok(taskService.updateTask(id, request));
////    }
//
//    public ResponseEntity<TaskResponse> updateTask(
//            @PathVariable Long id,
//            @Valid @RequestBody UpdateTaskRequest request
//    ) {
//        Task updatedTask = taskService.updateTask(id, request);
//        return ResponseEntity.ok(toResponse(updatedTask));
//    }
//
//    @DeleteMapping("/{id}")
////    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
////        taskService.deleteTask(id);
////        return ResponseEntity.noContent().build();
////    }
//
//    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
//        taskService.deleteTask(id);
//        return ResponseEntity.noContent().build();
//    }
//
//
//
//
//}

package com.example.todo.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    public ResponseEntity<TaskPageResponse> getAllTasks(
            @RequestParam(required = false) Boolean completed,

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
        Page<Task> taskPage = taskService.getAllTasks(
                completed,
                page,
                size,
                sortBy,
                direction
        );

        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }

    @GetMapping("/search")
    public ResponseEntity<TaskPageResponse> searchTasks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) TaskPriority priority,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueAfter,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueBefore,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate,

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
        Page<Task> taskPage = taskService.searchTasks(
                title,
                completed,
                priority,
                dueAfter,
                dueBefore,
                dueDate,
                page,
                size,
                sortBy,
                direction
        );

        return ResponseEntity.ok(taskMapper.toPageResponse(taskPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request
    ) {
        Task createdTask = taskService.createTask(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(taskMapper.toResponse(createdTask));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        Task updatedTask = taskService.updateTask(id, request);
        return ResponseEntity.ok(taskMapper.toResponse(updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> patchTask(@PathVariable Long id, @Valid @RequestBody PatchTaskRequest request){
        Task patchedTask = taskService.patchTask(id, request);
        return ResponseEntity.ok(taskMapper.toResponse(patchedTask));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(@PathVariable Long id) {
        Task task = taskService.completeTask(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TaskResponse> reopenTask(@PathVariable Long id) {
        Task task = taskService.reopenTask(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }



}
