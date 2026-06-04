# Project Folder Structure & Design Patterns Guide

> Task Management API — Spring Boot 4.x.x
> Package-by-Feature Architecture

---

## Table of Contents

1. [The Big Picture — Why This Structure?](#1-the-big-picture--why-this-structure)
2. [Full Folder Map](#2-full-folder-map)
3. [Package-by-Feature vs Package-by-Layer](#3-package-by-feature-vs-package-by-layer)
4. [The 7 Layers Inside Every Feature](#4-the-7-layers-inside-every-feature)
5. [Feature Deep Dive — task/](#5-feature-deep-dive--task)
6. [Feature Deep Dive — auth/](#6-feature-deep-dive--auth)
7. [Feature Deep Dive — user/](#7-feature-deep-dive--user)
8. [Feature Deep Dive — comment/](#8-feature-deep-dive--comment)
9. [The common/ Package](#9-the-common-package)
10. [Resources — Configuration and Database](#10-resources--configuration-and-database)
11. [Full Request-to-Response Workflow](#11-full-request-to-response-workflow)
12. [File-to-File Data Flow](#12-file-to-file-data-flow)
13. [Design Patterns in This Project](#13-design-patterns-in-this-project)
14. [When to Create Each File Type](#14-when-to-create-each-file-type)
15. [How Spring Wires Everything Together](#15-how-spring-wires-everything-together)

---

## 1. The Big Picture — Why This Structure?

Your project follows Package-by-Feature architecture. Every feature (task, user, auth, comment) is a self-contained package with its own controller, service, repository, entity, DTO, and mapper.

Think of it like a building:

```
Building = Your Application

  Floor 1 = auth feature      (register, login, logout)
  Floor 2 = task feature      (CRUD tasks, search, complete)
  Floor 3 = user feature      (profile, admin)
  Floor 4 = comment feature   (comments on tasks)

  Basement = common           (shared: security, exceptions, base entity)
```

Each floor is independent. If you remove the "comment floor", the rest of the building still stands.

---

## 2. Full Folder Map

```
src/
 main/
  java/com/example/todo/
   TodoApplication.java                      <- App entry point

   auth/                                     <- FEATURE: Authentication
    controller/
     AuthController.java
    dto/
     request/
      LoginRequest.java
      RegisterRequest.java
     response/
      LoginResponse.java
      RegisterResponse.java
    service/
     AuthService.java
     CustomUserDetailsService.java

   task/                                     <- FEATURE: Tasks
    controller/
     TaskController.java
    dto/
     request/
      CreateTaskRequest.java
      UpdateTaskRequest.java
      PatchTaskRequest.java
     response/
      TaskResponse.java
      TaskPageResponse.java
    entity/
     Task.java
     TaskPriority.java
    mapper/
     TaskMapper.java
    repository/
     TaskRepository.java
     TaskSpecifications.java
    service/
     TaskService.java

   user/                                     <- FEATURE: Users
    controller/
     UserController.java
    dto/
     response/
      UserResponse.java
      UserDetailResponse.java
    entity/
     AppUser.java
     UserRole.java
    mapper/
     UserMapper.java
    repository/
     UserRepository.java
    service/
     UserService.java

   comment/                                  <- FEATURE: Comments
    controller/
     TaskCommentController.java
    dto/
     request/
      CreateTaskCommentRequest.java
     response/
      TaskCommentResponse.java
    entity/
     TaskComment.java
    mapper/
     TaskCommentMapper.java
    repository/
     TaskCommentRepository.java
    service/
     TaskCommentService.java

   common/                                   <- SHARED: Used by all features
    entity/
     BaseEntity.java                         <- createdAt, updatedAt for all entities
    exception/
     ApiError.java                           <- Standard error response shape
     GlobalExceptionHandler.java             <- Catches all exceptions from all controllers
     TaskNotFoundException.java
     UserNotFoundException.java
     CommentNotFoundException.java
     DuplicateResourceException.java
     InvalidCredentialsException.java
     TaskAlreadyCompletedException.java
     BadRequestException.java
    security/
     SecurityConfig.java                     <- Security rules (who can access what)
     JwtService.java                         <- JWT create / read / validate
     JwtAuthFilter.java                      <- Reads JWT from every request
     CustomAuthenticationEntryPoint.java     <- Returns 401 JSON (not HTML redirect)

  resources/
   application.properties                    <- App configuration
   db/migration/
    V1__create_tasks_table.sql
    V2__add_priority_and_due_date_to_tasks.sql
    V3__create_users_and_assign_tasks.sql
    V4__create_task_comments_table.sql
    V5__add_password_to_users.sql
    V6__add_role_to_users.sql

 test/
  java/com/example/todo/
   auth/
    controller/AuthControllerTest.java
    service/AuthServiceTest.java
   common/
    TestJwtHelper.java
    security/JwtServiceTest.java
   integration/
    TaskIntegrationTest.java
   task/
    controller/TaskControllerTest.java
    repository/TaskRepositoryTest.java
    service/TaskServiceTest.java
   TodoApplicationTests.java
  resources/
   application-test.properties               <- Test database config
```

---

## 3. Package-by-Feature vs Package-by-Layer

### Package-by-Layer (old way - DO NOT use)

```
com.example.todo/
  controllers/       <- ALL controllers together
   AuthController.java
   TaskController.java
   UserController.java
  services/          <- ALL services together
   AuthService.java
   TaskService.java
   UserService.java
  repositories/      <- ALL repositories together
  entities/          <- ALL entities together
```

Problem: To work on "tasks" you jump between 4 folders. To delete "comments" you hunt across all folders.

### Package-by-Feature (this project - correct way)

```
com.example.todo/
  task/              <- EVERYTHING about tasks in one place
   controller/
   service/
   repository/
   entity/
   dto/
   mapper/
  auth/              <- EVERYTHING about auth in one place
  user/              <- EVERYTHING about users in one place
```

| | Package-by-Layer | Package-by-Feature |
|---|---|---|
| Related code location | Scattered across folders | Together in one folder |
| Adding a new feature | Edit many folders | Add one new folder |
| Deleting a feature | Hunt across all layers | Delete one folder |
| Team collaboration | Conflicts everywhere | Each developer owns a feature |
| Used in | Old tutorials | Real production projects |

---

## 4. The 7 Layers Inside Every Feature

Every feature follows the same internal structure. Here is what each layer does and why:

```
HTTP Request from client
        |
  [Controller]    <- Receives HTTP. Calls service. Returns HTTP response.
        |
  [Service]       <- Business logic. Decides WHAT to do.
        |
  [Repository]    <- Database access. Knows HOW to store data.
        |
  [Database]      <- PostgreSQL. Actual data storage.

Plus these support files exist alongside:

  [Entity]        <- Java class that maps to a database table
  [DTO]           <- What goes in/out via HTTP (not the entity!)
  [Mapper]        <- Converts Entity <-> DTO
```

### Layer Responsibilities

| Layer | Example file | Responsibility | Knows about |
|---|---|---|---|
| Controller | TaskController.java | Receive HTTP, call service, return response | DTOs, Service |
| Service | TaskService.java | Business rules and decisions | Repository, other Services |
| Repository | TaskRepository.java | Database queries | Entity, Database |
| Entity | Task.java | Database table structure | Database columns |
| DTO | TaskResponse.java | What the API sends/receives | Nothing - just data |
| Mapper | TaskMapper.java | Convert Entity to DTO and back | Entity, DTO |
| Specifications | TaskSpecifications.java | Dynamic query filters | Entity |

---

## 5. Feature Deep Dive - task/

### entity/Task.java - The Database Table

What it is: A Java class that maps 1-to-1 to the tasks table in PostgreSQL.

When to create: When you need to store new data. One entity = one table.

```java
@Entity                        // Hibernate: this class is a database table
@Table(name = "tasks")         // The table is named "tasks"
public class Task extends BaseEntity {  // inherits createdAt, updatedAt

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;            // -> id BIGSERIAL PRIMARY KEY

    @Column(nullable = false, length = 100)
    private String title;       // -> title VARCHAR(100) NOT NULL

    @Column(nullable = false)
    private boolean completed;  // -> completed BOOLEAN NOT NULL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority; // -> priority VARCHAR(20) stored as text

    @Column(name = "due_date")
    private LocalDate dueDate;  // -> due_date DATE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;       // -> user_id BIGINT FK to app_users

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskComment> comments; // related rows in task_comments table
}
```

Relationships explained:

```
@ManyToOne  -> Many tasks belong to ONE user
               Task table has user_id foreign key column

@OneToMany  -> ONE task has Many comments
               Comments table has task_id foreign key column
               cascade = ALL: delete task -> delete all its comments automatically
               orphanRemoval = true: remove comment from list -> delete from DB
```

Rule: NEVER send an entity directly to the client. Use a DTO. Why? Entities may contain:
- Sensitive fields (password)
- Circular references (task -> user -> tasks -> user...)
- Hibernate proxy objects that break JSON serialization

---

### entity/TaskPriority.java - Enum (Allowed Values)

```java
public enum TaskPriority {
    LOW, MEDIUM, HIGH
}
```

Why enum? Without it, someone could store "URGENT" or "urgent" in the database.
The enum ensures only LOW, MEDIUM, HIGH are ever valid.

How stored in DB: @Enumerated(EnumType.STRING) stores "LOW", "MEDIUM", "HIGH" as text.
This is safer than numbers - if you reorder the enum, number storage breaks.

---

### repository/TaskRepository.java - Database Access

What it is: The interface that talks to PostgreSQL. Spring Data JPA generates SQL automatically.

When to create: One repository per entity. Always.

```java
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // Spring reads the method name and generates SQL:
    // SELECT * FROM tasks LEFT JOIN app_users ON... WHERE completed = ? ORDER BY... LIMIT? OFFSET?
    @EntityGraph(attributePaths = "user")
    Page<Task> findByCompleted(boolean completed, Pageable pageable);

    // SELECT * FROM tasks LEFT JOIN app_users ON... WHERE user_id = ?
    @EntityGraph(attributePaths = "user")
    Page<Task> findByUserId(Long userId, Pageable pageable);

    // SELECT COUNT(*) FROM tasks WHERE user_id = ?
    Long countByUserId(Long userId);

    // Override to add EntityGraph (prevent LazyInitializationException)
    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Task> findById(Long id);
}
```

@EntityGraph explained:

```
Without @EntityGraph:
  Query 1: SELECT * FROM tasks WHERE completed = true  (10 tasks)
  Query 2: SELECT * FROM app_users WHERE id = 1        (for task 1)
  Query 3: SELECT * FROM app_users WHERE id = 2        (for task 2)
  ... 10 more queries = N+1 PROBLEM! Very slow!

With @EntityGraph(attributePaths = "user"):
  ONE query: SELECT tasks.*, users.* FROM tasks
             LEFT JOIN app_users ON app_users.id = tasks.user_id
             WHERE tasks.completed = true
  Loads everything in one shot. Fast!
```

Rule: Never put business logic in a repository. It only does database operations.

---

### repository/TaskSpecifications.java - Dynamic Filters

What it is: A class that builds SQL WHERE clauses dynamically at runtime.

When to use: When the user can filter by many optional parameters.

```java
// Each method is one piece of a WHERE clause
private static Specification<Task> titleContains(String title) {
    return (root, query, cb) -> {
        if (title == null || title.isBlank()) {
            return cb.conjunction(); // 1=1 = no filter (always true)
        }
        return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
        // -> WHERE LOWER(title) LIKE '%buy%'
    };
}

// Combine all pieces with AND
public static Specification<Task> withFilters(...) {
    return Specification
        .where(titleContains(title))
        .and(hasCompleted(completed))
        .and(hasPriority(priority))
        .and(dueDateAfterOrEqual(dueAfter));
}
```

When user sends: GET /api/v1/tasks/search?title=buy&priority=HIGH&dueAfter=2027-01-01
Generated SQL:
```sql
SELECT * FROM tasks
WHERE LOWER(title) LIKE '%buy%'
  AND priority = 'HIGH'
  AND due_date >= '2027-01-01'
ORDER BY id ASC LIMIT 5 OFFSET 0
```

---

### dto/request/CreateTaskRequest.java - Incoming Data

What it is: A plain Java class that represents the JSON the client sends.

When to create: One Request DTO per API write operation (create, update, patch).

```java
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100)
    private String title;          // maps from JSON "title"

    @NotNull(message = "Priority is required")
    private TaskPriority priority; // maps from JSON "priority"

    @FutureOrPresent
    private LocalDate dueDate;     // maps from JSON "dueDate"

    @NotNull(message = "User ID is required")
    private Long userId;           // maps from JSON "userId"
}
```

Validation annotations:
```
@NotBlank    -> must not be null and not empty string
@NotNull     -> must not be null
@Size        -> string length between min and max
@Email       -> must be valid email format
@FutureOrPresent -> date must be today or in the future
```

When the controller uses @Valid @RequestBody, Spring validates all rules before
the method runs. If any fails -> 400 Bad Request with field-level error messages.

---

### dto/response/TaskResponse.java - Outgoing Data

What it is: What the API sends back. The client sees this, not the entity.

```java
public class TaskResponse {
    private Long id;
    private String title;
    private boolean completed;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Long userId;         // from task.getUser().getId()
    private String userName;     // from task.getUser().getUsername()
    private Instant createdAt;
    private Instant updatedAt;
    // NO password field - we control exactly what the client sees
}
```

Why not return the entity directly?
```java
// BAD - entity has password, Hibernate proxies, circular refs
return taskRepository.findById(id).get();

// GOOD - DTO has only what client needs
return taskMapper.toResponse(taskService.getTaskById(id));
```

---

### mapper/TaskMapper.java - Entity to DTO Conversion

What it is: A Spring component that converts Task entities into TaskResponse DTOs.

When to create: One mapper per feature.

```java
@Component
public class TaskMapper {

    // Task entity -> TaskResponse DTO
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

    // Page<Task> -> TaskPageResponse (for paginated list endpoints)
    public TaskPageResponse toPageResponse(Page<Task> page) {
        List<TaskResponse> content = page.getContent()
            .stream().map(this::toResponse).toList();
        return new TaskPageResponse(content, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
```

Why a separate Mapper class?
- Controller does not know entity internals
- Service does not know about JSON format
- Conversion logic in one place - easy to change
- Easy to test in isolation

---

### service/TaskService.java - Business Logic

What it is: The brain of the feature. All business decisions and rules live here.

When to create: One service per feature.

```java
@Service
@Transactional(readOnly = true)  // all methods are read-only by default (faster)
public class TaskService {

    @Transactional  // overrides readOnly for this write method
    public Task createTask(CreateTaskRequest request) {
        // Rule 1: User must exist before creating a task
        AppUser user = userService.getUserById(request.getUserId());

        // Rule 2: New tasks ALWAYS start as incomplete (client cannot override this)
        Task task = new Task(request.getTitle(), false, request.getPriority(), ...);

        return taskRepository.save(task);
    }

    @Transactional
    public Task completeTask(Long id) {
        Task task = getTaskById(id);

        // Rule: Cannot complete an already-completed task
        if (task.isCompleted()) {
            throw new TaskAlreadyCompletedException(id);
        }

        task.setCompleted(true);
        return taskRepository.save(task);
    }
}
```

Service rules:
- Never access HttpServletRequest or HTTP concerns (controller's job)
- Never build JSON or worry about HTTP status codes (controller/mapper's job)
- Can call other services (TaskService calls UserService)
- Always enforce business rules here, not in the controller

---

### controller/TaskController.java - HTTP Entry Point

What it is: First receives HTTP request, last sends HTTP response. Nothing more.

Three responsibilities ONLY:
1. Receive the HTTP request
2. Call the service
3. Return the HTTP response with correct status code

```java
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
        @Valid @RequestBody CreateTaskRequest request
    ) {
        Task task = taskService.createTask(request);     // call service
        return ResponseEntity
            .status(HttpStatus.CREATED)                  // 201
            .body(taskMapper.toResponse(task));           // entity -> DTO -> JSON
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();        // 204, no body
    }
}
```

Controller rules:
- NEVER write business logic (no if/else rules here)
- NEVER query the database directly
- NEVER access the repository
- Keep methods short - just call service and return response

---

## 6. Feature Deep Dive - auth/

The auth feature is special - it has no repository of its own.
It reuses UserRepository from the user/ feature.

### service/AuthService.java

```
register():
  1. Check username not taken  (userRepository.existsByUsername)
  2. Check email not taken     (userRepository.existsByEmail)
  3. Hash the password         (passwordEncoder.encode)
  4. Save new user             (userRepository.save)

login():
  1. Find user by username     (userRepository.findByUsername)
  2. Verify password           (passwordEncoder.matches)
  3. Generate JWT token        (jwtService.generateToken)
  4. Return token + expiry
```

Security rule: Both "wrong username" and "wrong password" return the same error message
"Invalid username or password" - never reveal WHICH field was wrong.

### service/CustomUserDetailsService.java

What it is: The bridge between your AppUser and Spring Security.

Why it exists: Spring Security needs to load a user during JWT validation.
It calls loadUserByUsername(username) on this service.

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) {
        // Called by JwtAuthFilter on every authenticated request
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
```

---

## 7. Feature Deep Dive - user/

### entity/AppUser.java implements UserDetails

Why does the entity implement UserDetails?
Spring Security works with UserDetails objects. By implementing this interface,
AppUser IS a UserDetails - no conversion needed.

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    // Returns "ROLE_USER" or "ROLE_ADMIN"
    // Spring Security's hasRole("ADMIN") checks for "ROLE_ADMIN"
}
```

### Two access tiers in UserController

```
/api/v1/users/me         -> Any authenticated user (their own data)
/api/v1/users/{id}       -> ADMIN only (@PreAuthorize("hasRole('ADMIN')"))
```

### @AuthenticationPrincipal

```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getMe(
    @AuthenticationPrincipal AppUser currentUser  // Spring injects logged-in user
) {
    return ResponseEntity.ok(userMapper.toResponse(currentUser));
}
```

How Spring knows who the current user is:
JwtAuthFilter set it in SecurityContextHolder when it validated the JWT.
@AuthenticationPrincipal reads it from there automatically.

---

## 8. Feature Deep Dive - comment/

Comments are nested under tasks: /api/v1/tasks/{taskId}/comments

### Nested URL design

```java
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")  // taskId in the base URL
public class TaskCommentController {

    @GetMapping
    public List<TaskCommentResponse> getCommentsByTaskId(@PathVariable Long taskId) { }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable Long taskId,     // from URL: /tasks/5/comments
        @PathVariable Long commentId   // from URL: /tasks/5/comments/12
    ) { }
}
```

### Ownership validation in service

Before deleting a comment, verify it actually belongs to the given task:

```java
public void deleteComment(Long taskId, Long commentId) {
    taskService.getTaskById(taskId);  // verify task exists

    TaskComment comment = taskCommentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // Security check: comment must belong to this task
    if (!comment.getTask().getId().equals(taskId)) {
        throw new BadRequestException("Comment does not belong to task id: " + taskId);
    }

    taskCommentRepository.delete(comment);
}
```

### Cascade delete from Task

```java
// In Task.java:
@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
private List<TaskComment> comments;
```

When you delete a Task, ALL its comments are automatically deleted.
No separate delete step needed.

---

## 9. The common/ Package

common/ contains code used by ALL features. It is not a feature itself.

### common/entity/BaseEntity.java

What it is: Abstract parent class that every entity extends.
Auto-fills createdAt and updatedAt on every save.

```java
@MappedSuperclass          // not a table itself, but its fields go into child tables
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;   // auto-set when first saved

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;   // auto-updated on every save
}
```

@EnableJpaAuditing in TodoApplication.java activates this feature.

Without BaseEntity you would repeat this in EVERY entity:
```java
// Repeated in Task, AppUser, TaskComment...
private Instant createdAt;
private Instant updatedAt;
@PrePersist void onCreate() { createdAt = Instant.now(); }
@PreUpdate void onUpdate() { updatedAt = Instant.now(); }
```

With BaseEntity - write once, all entities inherit:
```java
public class Task extends BaseEntity { }        // has createdAt + updatedAt
public class AppUser extends BaseEntity { }     // has createdAt + updatedAt
public class TaskComment extends BaseEntity { } // has createdAt + updatedAt
```

---

### common/exception/GlobalExceptionHandler.java

What it is: Catches ALL exceptions from ALL controllers in one place.

Why: Without it you would write try-catch in every controller method.
With it, exceptions automatically become clean JSON responses.

```java
@RestControllerAdvice  // Applies to ALL @RestController classes
public class GlobalExceptionHandler {

    @ExceptionHandler({TaskNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFoundException(RuntimeException ex) {
        return ResponseEntity.status(404)
            .body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(400)
            .body(new ApiError(400, "Validation failed", errors));
    }
}
```

Flow without GlobalExceptionHandler:
```
Request -> Service throws TaskNotFoundException
-> Spring shows default 500 HTML error page (ugly, insecure)
```

Flow with GlobalExceptionHandler:
```
Request -> Service throws TaskNotFoundException
-> GlobalExceptionHandler catches it
-> Returns: { "status": 404, "message": "Task not found with id: 99" }
```

---

### common/exception/ApiError.java

Standard shape of ALL error responses:

```java
public class ApiError {
    private int status;                     // 404, 400, 401...
    private String message;                 // human-readable message
    private Map<String, String> errors;     // field-level validation errors
    private LocalDateTime timestamp;        // when the error occurred
}
```

Every error response looks the same:
```json
{ "status": 404, "message": "Task not found with id: 99", "errors": {}, "timestamp": "..." }
```

```json
{
  "status": 400, "message": "Validation failed",
  "errors": { "title": "Title is required", "userId": "User ID is required" }
}
```

---

### common/security/ - The 4 Security Files

These 4 files work together to protect every endpoint.

```
SecurityConfig.java              <- defines the rules (what is protected)
JwtService.java                  <- creates and reads JWT tokens
JwtAuthFilter.java               <- runs on every request, validates JWT
CustomAuthenticationEntryPoint   <- returns 401 JSON (not HTML redirect)
```

#### SecurityConfig.java - The Rules

```java
http
  .csrf(disabled)                           // stateless API, CSRF not needed
  .sessionManagement(STATELESS)             // no server sessions
  .authorizeHttpRequests(auth ->
    .requestMatchers("/api/v1/auth/**").permitAll()  // login/register: open to all
    .anyRequest().authenticated()                    // everything else: JWT required
  )
  .addFilterBefore(jwtAuthFilter, ...)      // check JWT before security decisions
  .exceptionHandling(                       // custom 401 JSON
    authenticationEntryPoint(customEntryPoint)
  );
```

#### JwtService.java - JWT Operations

```
generateToken(user)        -> Creates: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtdW55..."
                              Contains: username, issued time, expiry, signature

extractUsername(token)     -> Reads username from token payload

isTokenValid(token, user)  -> Returns true if:
                              1. username in token matches the user
                              2. token is not expired
                              Returns false for expired/malformed/wrong user
```

#### JwtAuthFilter.java - Runs on Every Request

```
Every request ->
  1. Read Authorization header
     If no "Bearer token" -> skip, let Spring Security handle it
  2. Extract token (remove "Bearer " prefix)
  3. Extract username from token
  4. Load AppUser from database by username
  5. Validate token (not expired + correct user)
  6. If valid -> set authentication in SecurityContextHolder
  7. Continue to controller
  If anything fails -> clear SecurityContext, continue
                       Spring Security rejects with 401
```

#### CustomAuthenticationEntryPoint.java - 401 Response

Without this: Spring redirects to /login page (HTML, useless for a REST API)
With this: Returns clean JSON: {"status":401,"message":"Authentication required"}

---

## 10. Resources - Configuration and Database

### application.properties

```properties
# Database connection
spring.datasource.url=jdbc:postgresql://localhost:5432/todo_db
spring.datasource.username=Muny
spring.datasource.password=Muny168168

# JPA - Hibernate validates schema (Flyway owns all schema changes)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.open-in-view=false    # close session after service returns

# Flyway - runs migration SQL files automatically on startup
spring.flyway.enabled=true

# JWT
jwt.secret=CdJeCU+qbKJVbIGBZrepVjuT9wmfbw9Mqmz2O4jo0+c=
jwt.expiration=86400000          # 24 hours in milliseconds

# CORS - which frontend URLs can call this API
cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

### db/migration/ - Flyway Migration Files

What is Flyway? A tool that manages database schema changes automatically.
Instead of manually running SQL scripts, Flyway runs them when the app starts.

Naming rule - MUST follow exactly:
```
V{version}__{description}.sql
  ^            ^
  Version      Double underscore, then description with underscores
```

| File | What it creates/changes |
|---|---|
| V1__create_tasks_table.sql | tasks table |
| V2__add_priority_and_due_date_to_tasks.sql | priority + due_date columns |
| V3__create_users_and_assign_tasks.sql | app_users table + user_id FK in tasks |
| V4__create_task_comments_table.sql | task_comments table |
| V5__add_password_to_users.sql | password column in app_users |
| V6__add_role_to_users.sql | role column in app_users |

GOLDEN RULE: NEVER edit an existing migration file.
Once run, it is permanent history. To change schema, add a new file:
```
V7__add_description_to_tasks.sql  <- new file for new changes
```

How Flyway works on startup:
```
1. App starts
2. Flyway reads flyway_schema_history table in the database
3. Sees V1 through V6 already ran
4. Looks for V7, V8... (none found today)
5. Schema is up to date -> app continues starting
```

---

## 11. Full Request-to-Response Workflow

Tracing: POST /api/v1/tasks (create a task)

```
CLIENT sends:
  POST /api/v1/tasks
  Authorization: Bearer eyJhbGc...
  Content-Type: application/json
  { "title": "Buy groceries", "priority": "HIGH", "dueDate": "2027-12-31", "userId": 1 }

STEP 1: JwtAuthFilter.java
  Reads: Authorization: Bearer eyJhbGc...
  Extracts token: "eyJhbGc..."
  Calls jwtService.extractUsername(token) -> "muny"
  Calls userDetailsService.loadUserByUsername("muny") -> AppUser object
  Calls jwtService.isTokenValid(token, user) -> true
  Sets SecurityContextHolder authentication
  Continues to next filter

STEP 2: SecurityConfig authorization check
  Path: /api/v1/tasks
  Rule: anyRequest().authenticated()
  SecurityContext: has authentication
  -> ALLOWED

STEP 3: TaskController.java receives request
  @PostMapping method is called
  Spring deserializes JSON -> CreateTaskRequest object
  @Valid triggers validation of all constraints

STEP 4: CreateTaskRequest validation
  title = "Buy groceries" -> @NotBlank OK, @Size(3-100) OK
  priority = HIGH -> @NotNull OK, valid enum OK
  dueDate = 2027-12-31 -> @FutureOrPresent OK
  userId = 1 -> @NotNull OK
  -> ALL PASS

STEP 5: TaskController calls TaskService
  Task createdTask = taskService.createTask(request);

STEP 6: TaskService.createTask() runs business logic
  Rule 1: User must exist
  AppUser user = userService.getUserById(1L);
  -> UserService calls UserRepository.findById(1L)
  -> SQL: SELECT * FROM app_users WHERE id = 1
  -> Returns AppUser(id=1, username="muny")

  Rule 2: New tasks always start incomplete
  Task task = new Task("Buy groceries", false, HIGH, 2027-12-31, user);
  taskRepository.save(task);

STEP 7: TaskRepository.save() hits database
  SQL: INSERT INTO tasks (title, completed, priority, due_date, user_id, created_at, updated_at)
       VALUES ('Buy groceries', false, 'HIGH', '2027-12-31', 1, NOW(), NOW())
  Returns Task entity with id = 42

STEP 8: Back in TaskController
  return ResponseEntity
    .status(HttpStatus.CREATED)         // 201
    .body(taskMapper.toResponse(task)); // convert entity -> DTO

STEP 9: TaskMapper.toResponse() converts Task -> TaskResponse
  Returns TaskResponse(42, "Buy groceries", false, HIGH, 2027-12-31, 1, "muny", ...)

STEP 10: HTTP Response sent to client
  HTTP/1.1 201 Created
  {
    "id": 42,
    "title": "Buy groceries",
    "completed": false,
    "priority": "HIGH",
    "dueDate": "2027-12-31",
    "userId": 1,
    "userName": "muny",
    "createdAt": "...",
    "updatedAt": "..."
  }
```

---

## 12. File-to-File Data Flow

### Creating a task - data flow diagram

```
HTTP Request (JSON string)
       |
       v
CreateTaskRequest.java      <- Jackson converts JSON string -> Java object
       |
       v (Spring validates @NotBlank, @NotNull etc.)
TaskController.java         <- receives CreateTaskRequest, calls taskService
       |
       v
TaskService.java            <- receives CreateTaskRequest, applies business rules
       |                       - calls UserService to verify user exists
       v
UserService.java            <- getUserById() -> calls UserRepository
       |
       v
UserRepository.java         <- findById(userId) -> SQL query -> AppUser
       |
       v (back up to TaskService)
TaskRepository.java         <- save(task) -> SQL INSERT -> Task with id
       |
       v (back up to TaskController)
TaskMapper.java             <- toResponse(Task entity) -> TaskResponse DTO
       |
       v
TaskResponse.java           <- Jackson converts Java object -> JSON string
       |
       v
HTTP Response (JSON string)
```

### Login - data flow diagram

```
HTTP Request: { "username": "muny", "password": "pass123" }
       |
       v
LoginRequest.java           <- deserialize JSON
       |
       v
AuthController.java         <- calls authService.login()
       |
       v
AuthService.java
  -> UserRepository.findByUsername("muny")       -> AppUser (with hashed password)
  -> passwordEncoder.matches("pass123", "$2a$")  -> true
  -> JwtService.generateToken(user)              -> "eyJhbGc..."
  -> returns LoginResponse("eyJhbGc...", 86400)
       |
       v
LoginResponse.java          <- serialize -> JSON
       |
       v
HTTP Response: { "accessToken": "eyJhbGc...", "tokenType": "Bearer", "expiresIn": 86400 }
```

### Error handling - data flow diagram

```
DELETE /api/v1/tasks/999
       |
       v
TaskController.deleteTask(999L)
       |
       v
TaskService.deleteTask(999L)
  -> taskRepository.findById(999L) -> Optional.empty()
  -> throws TaskNotFoundException(999L)
       |
       v (Spring intercepts automatically)
GlobalExceptionHandler.handleNotFoundException(ex)
  -> builds ApiError(404, "Task not found with id: 999")
       |
       v
HTTP Response: { "status": 404, "message": "Task not found with id: 999" }
```

---

## 13. Design Patterns in This Project

Design patterns are proven solutions to common programming problems.
Here are the ones you already use - and why they matter.

---

### Pattern 1: Repository Pattern

What: Hide all database access behind an interface.
The rest of the code never writes SQL directly.

Where in your project:
```java
// Interface - defines what operations are available
public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByCompleted(boolean completed, Pageable pageable);
}

// Service never writes SQL - only calls repository methods
public class TaskService {
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)   // no SQL here!
            .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
```

Why it matters:
- You can swap PostgreSQL for MySQL without changing TaskService
- You can mock the repository in tests (no real database needed)
- All SQL is in one place - easy to find and fix

---

### Pattern 2: DTO Pattern (Data Transfer Object)

What: Use separate objects for network communication and database storage.
Never expose your database entities directly to the API.

Where in your project:
```
Database layer:  Task.java         <- has ALL fields: id, title, user, comments, password link...
API layer:       TaskResponse.java <- has ONLY what client needs: id, title, completed...

Mapper bridges them:
TaskMapper.toResponse(Task) -> TaskResponse
```

Why it matters:
- Hides sensitive fields (password in AppUser never leaks)
- You can change database schema without breaking the API contract
- Different endpoints can have different DTOs from the same entity
- Prevents clients from sending fields they should not control

---

### Pattern 3: Mapper Pattern

What: A dedicated class responsible only for converting between two types.

Where in your project:
```java
@Component
public class TaskMapper {
    // One job: convert between Entity and DTO
    public TaskResponse toResponse(Task task) { ... }
    public TaskPageResponse toPageResponse(Page<Task> page) { ... }
}
```

Why it matters:
- Controller does not know entity structure
- Service does not know JSON format
- Conversion logic in one place - change once, works everywhere

---

### Pattern 4: Service Layer Pattern

What: All business logic lives in service classes, separate from HTTP and database.

Where in your project:
```java
// Controller: ONLY HTTP concerns
public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest req) {
    Task task = taskService.createTask(req);              // delegate to service
    return ResponseEntity.created(taskMapper.toResponse(task));
}

// Service: ALL business rules
public Task createTask(CreateTaskRequest request) {
    AppUser user = userService.getUserById(request.getUserId()); // rule: user must exist
    Task task = new Task(request.getTitle(), false, ...);        // rule: always starts false
    return taskRepository.save(task);
}
```

Why it matters:
- Business rules are testable without HTTP or database (unit tests)
- Same service can be called from multiple places
- Clear separation - you know exactly where to find a rule

---

### Pattern 5: Specification Pattern

What: Encapsulate each query filter as an object. Combine them dynamically.

Where in your project:
```java
// Each Specification is one filter piece
Specification<Task> combined = Specification
    .where(titleContains("buy"))              // WHERE title LIKE '%buy%'
    .and(hasPriority(TaskPriority.HIGH))      // AND priority = 'HIGH'
    .and(dueDateAfterOrEqual(LocalDate.now())); // AND due_date >= TODAY

taskRepository.findAll(combined, pageable);
```

Why it matters:
- No need for findByTitleAndPriority(), findByTitleAndDate(), etc. (and all combinations)
- Null filters are ignored automatically (no filter = no SQL condition)
- Each filter is independently testable

---

### Pattern 6: Chain of Responsibility (Security Filter Chain)

What: A chain of handlers. Each processes the request and passes to the next.

Where in your project:
```
Request ->
  Filter 1: CORS check
  Filter 2: JwtAuthFilter  (reads token, sets who is logged in)
  Filter 3: Authorization  (checks if logged-in user can access this URL)
  Filter 4: DispatcherServlet (routes to the correct controller)
  -> Controller
```

Each filter has one responsibility. If JwtAuthFilter fails (bad token),
it clears authentication and the request falls through to authorization check,
which rejects it with 401.

---

### Pattern 7: Template Method Pattern (BaseEntity)

What: Define the skeleton in a parent class, children fill in the details.

Where in your project:
```java
// Parent defines: every entity MUST have timestamps
@MappedSuperclass
public abstract class BaseEntity {
    @CreatedDate   private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}

// Children get timestamps for free
public class Task extends BaseEntity { }
public class AppUser extends BaseEntity { }
public class TaskComment extends BaseEntity { }
```

Why it matters:
- Write timestamp logic once, used by all entities
- Consistent createdAt/updatedAt across the entire application
- Add more shared fields to BaseEntity -> all entities get them

---

### Pattern 8: Strategy Pattern (PasswordEncoder)

What: Define a family of algorithms, make them interchangeable.
The caller does not care which algorithm is used.

Where in your project:
```java
// PasswordEncoder is a strategy interface
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();  // strategy implementation
    // Could swap to Argon2PasswordEncoder without changing AuthService
}

// AuthService uses the strategy interface - not the implementation
public class AuthService {
    private final PasswordEncoder passwordEncoder; // interface!

    public AppUser register(RegisterRequest req) {
        String hashed = passwordEncoder.encode(req.getPassword()); // call strategy
    }
}
```

Why it matters:
- To change from BCrypt to Argon2: change ONE line in SecurityConfig
- Nothing else changes - AuthService is unchanged
- Easy to test: mock the PasswordEncoder interface

---

### Pattern 9: Constructor Injection (Dependency Injection Pattern)

What: Instead of creating dependencies with new, let Spring inject them.

Where in your project:
```java
@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserService userService;

    // Spring reads this constructor and injects the matching beans automatically
    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }
}
```

Why constructor injection over @Autowired on fields?
```java
// BAD - field injection (hidden dependencies, hard to test)
@Autowired
private TaskRepository taskRepository;

// GOOD - constructor injection (explicit, testable, immutable)
public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
}
// In tests: new TaskService(mockRepository) - simple!
```

---

## 14. When to Create Each File Type

| You need to... | Create... | Location |
|---|---|---|
| Store data in a database | {Name}Entity.java | {feature}/entity/ |
| Define allowed values for a field | {Name}Enum.java | {feature}/entity/ |
| Read/write entities in the database | {Name}Repository.java | {feature}/repository/ |
| Build dynamic query filters | {Name}Specifications.java | {feature}/repository/ |
| Write business logic and rules | {Name}Service.java | {feature}/service/ |
| Receive data from client (request body) | {Name}Request.java | {feature}/dto/request/ |
| Send data to client (response body) | {Name}Response.java | {feature}/dto/response/ |
| Convert Entity to/from DTO | {Name}Mapper.java | {feature}/mapper/ |
| Expose HTTP endpoints (URLs) | {Name}Controller.java | {feature}/controller/ |
| Add a new custom error type | {Name}Exception.java | common/exception/ |
| Add shared fields to all entities | Extend BaseEntity.java | common/entity/ |
| Create or modify a database table | V{n}__{description}.sql | resources/db/migration/ |
| Add configuration | application.properties | resources/ |

---

## 15. How Spring Wires Everything Together

You never write new TaskService() in production code. Spring does it for you.

### Annotations that create Spring beans

| Annotation | Where used | What Spring does |
|---|---|---|
| @RestController | Controller classes | Creates bean, handles HTTP routing |
| @Service | Service classes | Creates bean, marks as business logic layer |
| @Repository | (implied by JpaRepository) | Creates bean, marks as data access layer |
| @Component | Mapper, Filter classes | Creates a general-purpose bean |
| @Configuration | Config classes | Creates bean, used to define other beans |
| @Bean | Methods in @Configuration | The method's return value becomes a Spring bean |

### Constructor Injection - how Spring wires beans

```java
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }
}
```

Spring sees TaskService needs:
1. A TaskRepository bean -> Spring Data JPA generated it -> inject
2. A UserService bean -> Spring found it -> inject

You never call new TaskService() anywhere. Spring creates it and injects everything.

### App startup sequence

```
1. Spring Boot starts
2. Flyway reads db/migration/ -> runs any pending SQL migrations
3. Hibernate validates schema matches your entities
4. Spring scans for @Component, @Service, @Controller, @Repository
5. Spring creates all beans and injects dependencies (wiring)
6. SecurityConfig builds the filter chain
7. App is ready, listening on port 8080
```

### The Dependency Graph (simplified)

```
TodoApplication
    |
    +-> SecurityConfig
    |       |-> JwtAuthFilter
    |       |       |-> JwtService
    |       |       +-> CustomUserDetailsService
    |       |               +-> UserRepository
    |       +-> CustomAuthenticationEntryPoint
    |
    +-> TaskController
    |       |-> TaskService
    |       |       |-> TaskRepository
    |       |       +-> UserService
    |       |               +-> UserRepository
    |       +-> TaskMapper
    |
    +-> AuthController
    |       +-> AuthService
    |               |-> UserRepository
    |               |-> PasswordEncoder
    |               +-> JwtService
    |
    +-> UserController
    |       |-> UserService
    |       +-> UserMapper
    |
    +-> GlobalExceptionHandler (catches exceptions from ALL controllers)
```

---

*Guide complete.*
*Every file has exactly one responsibility.*
*Every layer only knows what it needs to know.*
*That is what makes this architecture clean and maintainable.*
