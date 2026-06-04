# Spring Boot Step One — REST API Foundation

**Target version:** Spring Boot 4.0.6  
**Project style:** Backend REST API  
**Current storage:** In-memory `ArrayList`  
**Next major step after this:** Spring Data JPA + PostgreSQL

> Package used in this note: `com.example.todo`  
> If your package is different, replace `com.example.todo` with your actual package name.

---

## Step One Completion Status

You completed Step One when these features work:

- You can run the Spring Boot project.
- You created a simple `GET /` or `GET /api/hello` endpoint.
- You built a Task REST API with:
  - `GET /api/tasks`
  - `GET /api/tasks/{id}`
  - `POST /api/tasks`
  - `PUT /api/tasks/{id}`
  - `DELETE /api/tasks/{id}`
- You separated controller logic from service logic.
- You used `ResponseEntity` for correct HTTP status codes.
- You validated request bodies using `jakarta.validation`.
- You created clean validation error responses.
- You created a custom `TaskNotFoundException`.
- You returned `TaskResponse` DTO instead of returning internal model directly.

If all of these work, **Step One is complete**.

---

## Spring Boot 4 Important Notes

Spring Boot 4 uses the modern Jakarta ecosystem. That means validation imports should use:

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
```

Do **not** use the old Java EE imports:

```java
import javax.validation.Valid;
```

For this step, you only need these main dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Do not manually write dependency versions unless there is a special reason. Spring Boot manages compatible dependency versions for you.

---

# Lesson 1 — First REST Endpoint

## Goal

Create the smallest REST API endpoint and understand the basic Spring Boot flow.

## `HelloController.java`

```java
package com.example.todo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "Hello Spring Boot";
    }

    @GetMapping("/api/hello")
    public String apiHello() {
        return "Hello from REST API";
    }
}
```

## Explanation

### `@RestController`

`@RestController` tells Spring that this class handles web API requests and returns data directly as the HTTP response body.

Khmer:  
`@RestController` ប្រាប់ Spring ថា class នេះជា API controller សម្រាប់ទទួល request និងផ្ញើ response ត្រឡប់ទៅ client។

### `@GetMapping`

`@GetMapping` handles HTTP `GET` requests.

Example:

```text
GET http://localhost:8080/api/hello
```

Expected result:

```text
Hello from REST API
```

## Golden Rule

```text
@RestController = class for API endpoints
@GetMapping = handle HTTP GET requests
Return value = response body sent to client
```

---

# Lesson 2 — Task REST API with ArrayList

## Goal

Create a basic Task API using `ArrayList` as temporary storage before learning database.

## File Structure

```text
src/main/java/com/example/todo
  TodoApplication.java
  HelloController.java

  task
    Task.java
    CreateTaskRequest.java
    UpdateTaskRequest.java
    TaskController.java
```

---

## `Task.java`

```java
package com.example.todo.task;

public class Task {
    private Long id;
    private String title;
    private boolean completed;

    public Task(Long id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
```

## Explanation

`Task` is the internal model for one task.

Khmer:  
`Task` គឺជា class តំណាងឱ្យ task មួយនៅក្នុង backend។

---

## `CreateTaskRequest.java`

```java
package com.example.todo.task;

public class CreateTaskRequest {
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
```

This receives JSON for creating a task:

```json
{
  "title": "Learn Spring Boot"
}
```

---

## `UpdateTaskRequest.java`

```java
package com.example.todo.task;

public class UpdateTaskRequest {
    private String title;
    private boolean completed;

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
```

This receives JSON for updating a task:

```json
{
  "title": "Learn Spring Controller",
  "completed": true
}
```

---

## Beginner `TaskController.java`

This was the first working controller before service layer:

```java
package com.example.todo.task;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final List<Task> tasks = new ArrayList<>();
    private Long nextId = 1L;

    @GetMapping
    public List<Task> getAllTasks() {
        return tasks;
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable Long id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @PostMapping
    public Task createTask(@RequestBody CreateTaskRequest request) {
        Task task = new Task(nextId, request.getTitle(), false);
        tasks.add(task);
        nextId++;
        return task;
    }

    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable Long id,
            @RequestBody UpdateTaskRequest request
    ) {
        Task task = tasks.stream()
                .filter(existingTask -> existingTask.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (task == null) {
            return null;
        }

        task.setTitle(request.getTitle());
        task.setCompleted(request.isCompleted());

        return task;
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long id) {
        boolean removed = tasks.removeIf(task -> task.getId().equals(id));

        if (removed) {
            return "Task deleted successfully";
        }

        return "Task not found";
    }
}
```

## Golden Rule

```text
ArrayList = fake temporary database for learning
Real database comes later with Spring Data JPA
```

---

# Lesson 3 — Service Layer + Dependency Injection

## Goal

Separate API routing from business logic.

Bad structure:

```text
Controller = API + business logic + storage logic
```

Better structure:

```text
Controller = HTTP request and response
Service = business logic
Repository = database later
```

---

## `TaskService.java`

```java
package com.example.todo.task;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private Long nextId = 1L;

    public List<Task> getAllTasks() {
        return tasks;
    }

    public Task getTaskById(Long id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public Task createTask(CreateTaskRequest request) {
        Task task = new Task(nextId, request.getTitle(), false);
        tasks.add(task);
        nextId++;
        return task;
    }

    public Task updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskById(id);

        if (task == null) {
            return null;
        }

        task.setTitle(request.getTitle());
        task.setCompleted(request.isCompleted());

        return task;
    }

    public boolean deleteTask(Long id) {
        return tasks.removeIf(task -> task.getId().equals(id));
    }
}
```

---

## Controller with Service Injection

```java
package com.example.todo.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping
    public Task createTask(@RequestBody CreateTaskRequest request) {
        return taskService.createTask(request);
    }

    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable Long id,
            @RequestBody UpdateTaskRequest request
    ) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long id) {
        boolean removed = taskService.deleteTask(id);

        if (removed) {
            return "Task deleted successfully";
        }

        return "Task not found";
    }
}
```

## Explanation

### `@Service`

`@Service` tells Spring that this class contains business logic and should be managed as a Spring Bean.

Khmer:  
`@Service` ប្រាប់ Spring ថា class នេះជា service សម្រាប់ដាក់ business logic។

### Constructor Injection

```java
public TaskController(TaskService taskService) {
    this.taskService = taskService;
}
```

Spring automatically gives `TaskService` to `TaskController`.

Khmer:  
Spring ជាអ្នកបង្កើត `TaskService` ហើយបញ្ចូលទៅក្នុង `TaskController` ដោយស្វ័យប្រវត្តិ។

## Golden Rule

```text
Controller should not contain business logic.
Controller receives request and returns response.
Service contains business logic.
Repository will handle database later.
```

---

# Lesson 4 — ResponseEntity + HTTP Status Codes

## Goal

Return correct HTTP status codes instead of only data or text.

---

## Improved `TaskController.java` with `ResponseEntity`

```java
package com.example.todo.task;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(task);
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody CreateTaskRequest request) {
        Task createdTask = taskService.createTask(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody UpdateTaskRequest request
    ) {
        Task updatedTask = taskService.updateTask(id, request);

        if (updatedTask == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        boolean removed = taskService.deleteTask(id);

        if (!removed) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
```

## Important HTTP Status Codes

| Status | Meaning | When to use |
|---|---|---|
| `200 OK` | Success | Get/update success |
| `201 Created` | Created new data | After `POST` |
| `204 No Content` | Success with no body | After `DELETE` |
| `400 Bad Request` | Client sent invalid data | Validation errors |
| `404 Not Found` | Resource not found | Wrong ID |
| `500 Internal Server Error` | Server bug | Unexpected error |

## Golden Rule

```text
ResponseEntity<T> = HTTP status + response body
ResponseEntity<Void> = HTTP status only, no body
```

Specific REST rule:

```text
GET success     → 200 OK
POST success    → 201 Created
PUT success     → 200 OK
DELETE success  → 204 No Content
Not found       → 404 Not Found
Invalid request → 400 Bad Request
```

---

# Lesson 5 — Request Validation

## Goal

Reject bad client input before service logic runs.

Bad request examples:

```json
{
  "title": ""
}
```

```json
{}
```

```json
{
  "title": "Hi"
}
```

These should return:

```text
400 Bad Request
```

---

## Add Validation Dependency

In `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Updated `CreateTaskRequest.java`

```java
package com.example.todo.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
```

---

## Updated `UpdateTaskRequest.java`

```java
package com.example.todo.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    private boolean completed;

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
```

---

## Add `@Valid` in Controller

```java
import jakarta.validation.Valid;
```

```java
@PostMapping
public ResponseEntity<Task> createTask(@Valid @RequestBody CreateTaskRequest request) {
    Task createdTask = taskService.createTask(request);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdTask);
}
```

```java
@PutMapping("/{id}")
public ResponseEntity<Task> updateTask(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTaskRequest request
) {
    Task updatedTask = taskService.updateTask(id, request);

    if (updatedTask == null) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(updatedTask);
}
```

## Golden Rule

```text
Validation annotations go inside request DTOs.
@Valid goes in the controller method parameter.
Bad client input should return 400 Bad Request.
```

---

# Lesson 6 — Clean Error Response

## Goal

Convert validation errors into clean JSON.

Expected response:

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "title": "Title is required"
  },
  "timestamp": "2026-05-01T..."
}
```

---

## File Structure

```text
src/main/java/com/example/todo
  common
    exception
      ApiError.java
      GlobalExceptionHandler.java
```

---

## `ApiError.java`

```java
package com.example.todo.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiError {
    private int status;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public ApiError(int status, String message, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
```

---

## `GlobalExceptionHandler.java`

```java
package com.example.todo.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }
}
```

## Explanation

### `@RestControllerAdvice`

Global error handler for REST controllers.

Khmer:  
`@RestControllerAdvice` ប្រើសម្រាប់ចាប់ error ពី controller ទាំងអស់ ហើយបម្លែងទៅជា JSON response ស្អាត។

### `@ExceptionHandler`

Handles a specific exception type.

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
```

Meaning:

```text
When @Valid fails, run this method.
```

## Golden Rule

```text
DTO contains validation rules.
Controller uses @Valid.
GlobalExceptionHandler formats validation errors.
```

---

# Lesson 7 — Custom TaskNotFoundException

## Goal

Stop returning `null` for not-found cases.

Bad:

```java
return null;
```

Better:

```java
throw new TaskNotFoundException(id);
```

---

## `TaskNotFoundException.java`

```java
package com.example.todo.task;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Task not found with id: " + id);
    }
}
```

---

## Improved `ApiError.java`

```java
package com.example.todo.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiError {
    private int status;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public ApiError(int status, String message) {
        this(status, message, Map.of());
    }

    public ApiError(int status, String message, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
```

---

## Updated `TaskService.java`

```java
package com.example.todo.task;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private Long nextId = 1L;

    public List<Task> getAllTasks() {
        return tasks;
    }

    public Task getTaskById(Long id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task createTask(CreateTaskRequest request) {
        Task task = new Task(nextId, request.getTitle(), false);
        tasks.add(task);
        nextId++;
        return task;
    }

    public Task updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskById(id);

        task.setTitle(request.getTitle());
        task.setCompleted(request.isCompleted());

        return task;
    }

    public void deleteTask(Long id) {
        boolean removed = tasks.removeIf(task -> task.getId().equals(id));

        if (!removed) {
            throw new TaskNotFoundException(id);
        }
    }
}
```

Important correction:

```text
If method return type is boolean, it must return true/false.
If failure is handled by exception and success returns no data, use void.
```

So this is correct:

```java
public void deleteTask(Long id)
```

---

## Updated `GlobalExceptionHandler.java`

```java
package com.example.todo.common.exception;

import com.example.todo.task.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiError> handleTaskNotFoundException(
            TaskNotFoundException ex
    ) {
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(apiError);
    }
}
```

## Golden Rule

```text
Do not return null for important API errors.
Throw a custom exception instead.
Let GlobalExceptionHandler convert exceptions into clean HTTP responses.
```

---

# Lesson 8 — Response DTO Layer

## Goal

Stop returning internal model directly from the API.

Use:

```text
Request DTO  = data coming into the API
Response DTO = data going out of the API
Model        = internal backend data
```

---

## `TaskResponse.java`

```java
package com.example.todo.task;

public class TaskResponse {
    private Long id;
    private String title;
    private boolean completed;

    public TaskResponse(Long id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }
}
```

---

## Final Step One `TaskController.java`

```java
package com.example.todo.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<TaskResponse> response = taskService.getAllTasks()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(toResponse(task));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request
    ) {
        Task createdTask = taskService.createTask(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(createdTask));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        Task updatedTask = taskService.updateTask(id, request);
        return ResponseEntity.ok(toResponse(updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.isCompleted()
        );
    }
}
```

## Golden Rule

```text
Do not expose internal models/entities directly in real APIs.
Return response DTOs instead.
```

---

# Final Step One Code Checklist

At the end of Step One, you should have these files:

```text
src/main/java/com/example/todo
  TodoApplication.java
  HelloController.java

  task
    Task.java
    TaskController.java
    TaskService.java
    CreateTaskRequest.java
    UpdateTaskRequest.java
    TaskResponse.java
    TaskNotFoundException.java

  common
    exception
      ApiError.java
      GlobalExceptionHandler.java
```

---

# Final Postman Test Checklist

Run the app:

```powershell
.\mvnw spring-boot:run
```

Base URL:

```text
http://localhost:8080
```

## 1. Get all tasks

```text
GET /api/tasks
```

Expected:

```text
200 OK
```

Body:

```json
[]
```

## 2. Create task

```text
POST /api/tasks
```

Body:

```json
{
  "title": "Learn Spring Boot Step One"
}
```

Expected:

```text
201 Created
```

Body:

```json
{
  "id": 1,
  "title": "Learn Spring Boot Step One",
  "completed": false
}
```

## 3. Get one task

```text
GET /api/tasks/1
```

Expected:

```text
200 OK
```

## 4. Update task

```text
PUT /api/tasks/1
```

Body:

```json
{
  "title": "Finish Step One",
  "completed": true
}
```

Expected:

```text
200 OK
```

## 5. Delete task

```text
DELETE /api/tasks/1
```

Expected:

```text
204 No Content
```

## 6. Get task not found

```text
GET /api/tasks/999
```

Expected:

```text
404 Not Found
```

Expected body:

```json
{
  "status": 404,
  "message": "Task not found with id: 999",
  "errors": {},
  "timestamp": "..."
}
```

## 7. Validate empty title

```text
POST /api/tasks
```

Body:

```json
{
  "title": ""
}
```

Expected:

```text
400 Bad Request
```

Expected body:

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "title": "Title is required"
  },
  "timestamp": "..."
}
```

## 8. Validate short title

```text
POST /api/tasks
```

Body:

```json
{
  "title": "Hi"
}
```

Expected:

```text
400 Bad Request
```

Expected body:

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "title": "Title must be between 3 and 100 characters"
  },
  "timestamp": "..."
}
```

---

# Step One Golden Summary

```text
URL identifies the resource.
HTTP method identifies the action.
Return type identifies the response body.
ResponseEntity controls status code and response body.
DTO separates API input/output from internal backend model.
Service contains business logic.
Controller handles HTTP request/response.
Exception represents business error.
GlobalExceptionHandler converts exceptions to clean JSON.
```

---

# What Comes Next?

Step Two should be:

```text
Spring Data JPA + PostgreSQL
```

In Step Two, we will replace:

```java
private final List<Task> tasks = new ArrayList<>();
```

with:

```java
private final TaskRepository taskRepository;
```

And we will learn:

- Entity
- Repository
- PostgreSQL connection
- `application.properties`
- `@Id`
- `@GeneratedValue`
- `JpaRepository`
- database CRUD
- schema/table creation
- real persistent data

---

# Official References Used

- Spring Boot System Requirements: https://docs.spring.io/spring-boot/system-requirements.html
- Spring Boot 4 Release Highlights: https://spring.io/projects/release-highlights
- Spring Framework REST Error Responses: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- Spring MVC Return Types: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html
