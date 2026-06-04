# Spring Boot Step 2 — PostgreSQL + Spring Data JPA Review Notes

**Project:** Task Management API  
**Target:** Spring Boot 4.x.x / Spring Framework 7.x  
**Database:** PostgreSQL  
**Build tool:** Maven  
**Status:** Step 2 completed for current learning level

---

## Step 2 Goal

In Step 1, the project used an in-memory `ArrayList`.

In Step 2, we replaced beginner storage with a real PostgreSQL database and learned how Spring Boot works with Spring Data JPA, Hibernate, Flyway, transactions, relationships, and clean error handling.

---

## Step 2 Completed Topics

1. PostgreSQL connection
2. JDBC driver concept
3. Spring Data JPA
4. Hibernate entity mapping
5. Primary key and auto-generated ID
6. Repository CRUD
7. Filtering
8. Pagination and sorting
9. Safe request parameters
10. Entity auditing: `createdAt`, `updatedAt`
11. Flyway migrations
12. V2 migration: `priority`, `dueDate`
13. Mapper pattern
14. `PUT` vs `PATCH`
15. JPQL search
16. Specification dynamic search
17. Transactions
18. User and Task relationship
19. `GET /api/users/{id}/tasks`
20. N+1 query problem
21. Fetch join
22. `@EntityGraph`
23. Safe `@OneToMany`
24. Cascade and `orphanRemoval`
25. Task comments
26. Professional custom exceptions

---

# Lesson 1 — PostgreSQL Connection

## What you learned

You learned how to connect Spring Boot to PostgreSQL using Spring Data JPA and the PostgreSQL JDBC driver.

## Simple explanation

Spring Boot does not talk to PostgreSQL directly. It uses this flow:

```text
Spring Boot → Spring Data JPA → Hibernate → JDBC Driver → PostgreSQL
```

## Khmer explanation

Spring Boot មិនភ្ជាប់ទៅ PostgreSQL ដោយផ្ទាល់ទេ។ វាប្រើ Hibernate និង JDBC Driver ដើម្បីទាក់ទងជាមួយ database។

## Maven dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

You do **not** need to manually download JDBC from a browser. Maven downloads it automatically.

## `application.properties`

```properties
spring.application.name=todo

spring.datasource.url=jdbc:postgresql://localhost:5432/todo_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

After Flyway, we changed this:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

## Common error

```text
Unable to determine Dialect without JDBC metadata
```

Usually means Spring Boot cannot connect to PostgreSQL, the datasource URL/password is wrong, PostgreSQL is not running, or the dependency is missing.

## Test remote database port

```powershell
Test-NetConnection 192.254.10.124 -Port 5432
```

Expected:

```text
TcpTestSucceeded : True
```

---

# Lesson 2 — Convert `Task` to JPA Entity

## What you learned

You converted your normal Java class into a database-mapped entity.

## Code

```java
package com.example.todo.task;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private boolean completed;

    protected Task() {
    }

    public Task(String title, boolean completed) {
        this.title = title;
        this.completed = completed;
    }
}
```

## Code explanation

`@Entity` maps the class to a database table.

`@Table(name = "tasks")` maps it to the `tasks` table.

`@Id` marks the primary key.

`@GeneratedValue(strategy = GenerationType.IDENTITY)` lets PostgreSQL generate the ID.

## Golden rule

```text
Entity = Java class mapped to database table
Field = database column
@Id = primary key
```

---

# Lesson 3 — `TaskRepository`

## What you learned

You created a repository to talk to the database.

## Code

```java
package com.example.todo.task;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
```

## What `JpaRepository` gives you

```text
findAll()
findById(id)
save(entity)
delete(entity)
existsById(id)
count()
```

## Golden rule

```text
Controller → Service → Repository → PostgreSQL
```

---

# Lesson 4 — Replace `ArrayList` with PostgreSQL

## What you learned

You replaced this:

```java
private final List<Task> tasks = new ArrayList<>();
```

with this:

```java
private final TaskRepository taskRepository;
```

## Service logic

```java
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task createTask(CreateTaskRequest request) {
        Task task = new Task(request.getTitle(), false);
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskById(id);
        task.setTitle(request.getTitle());
        task.setCompleted(request.isCompleted());
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }
}
```

## Golden rule

```text
Service contains business logic.
Repository performs database operations.
```

---

# Lesson 5 — Filtering with Repository Methods

## What you learned

You added filtering using Spring Data derived query methods.

## Repository

```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCompleted(boolean completed);

    List<Task> findByTitleContainingIgnoreCase(String title);
}
```

## Explanation

`findByCompleted(true)` means:

```sql
WHERE completed = true
```

`findByTitleContainingIgnoreCase("spring")` means a case-insensitive title search.

## Golden rule

```text
Derived query method = query created from method name
```

---

# Lesson 6 — Pagination and Sorting

## What you learned

Real APIs should not return unlimited rows.

## Important classes

```java
Page<T>
Pageable
PageRequest
Sort
```

## Service method

```java
public Page<Task> getAllTasks(
        Boolean completed,
        int page,
        int size,
        String sortBy,
        String direction
) {
    Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);

    if (completed == null) {
        return taskRepository.findAll(pageable);
    }

    return taskRepository.findByCompleted(completed, pageable);
}
```

## `TaskPageResponse`

```java
public class TaskPageResponse {
    private List<TaskResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
```

## Test

```http
GET /api/tasks?page=0&size=5&sortBy=id&direction=asc
```

## Golden rule

```text
Page index starts at 0.
Page<T> = data + pagination metadata.
Never return unlimited rows in real APIs.
```

---

# Lesson 7 — Safe Pagination and Sorting

## What you learned

You protected the API from bad query parameters.

## Controller validation

```java
@RequestParam(defaultValue = "0")
@Min(value = 0, message = "Page must be 0 or greater")
int page,

@RequestParam(defaultValue = "5")
@Min(value = 1, message = "Size must be at least 1")
@Max(value = 50, message = "Size must not be greater than 50")
int size,
```

## Safe sort field whitelist

```java
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
```

## Safe sort direction

```java
private Sort.Direction validateDirection(String direction) {
    if (direction.equalsIgnoreCase("asc")) {
        return Sort.Direction.ASC;
    }

    if (direction.equalsIgnoreCase("desc")) {
        return Sort.Direction.DESC;
    }

    throw new IllegalArgumentException("Invalid sort direction: " + direction);
}
```

## Golden rule

```text
Never trust query parameters.
Whitelist sort fields.
Whitelist sort directions.
```

---

# Lesson 8 — Entity Auditing

## What you learned

You added automatic timestamps:

```text
createdAt
updatedAt
```

## Enable auditing

```java
@SpringBootApplication
@EnableJpaAuditing
public class TodoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
```

## Entity fields

```java
@CreatedDate
@Column(name = "created_at", nullable = false, updatable = false)
private Instant createdAt;

@LastModifiedDate
@Column(name = "updated_at", nullable = false)
private Instant updatedAt;
```

## Add entity listener

```java
@EntityListeners(AuditingEntityListener.class)
```

## Common mistake

If Flyway creates `created_at`, your entity must map to `created_at`:

```java
@Column(name = "created_at")
private Instant createdAt;
```

## Golden rule

```text
Flyway column names and entity @Column names must match exactly.
```

---

# Lesson 9 — Flyway Migration

## What you learned

Flyway is database version control.

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

## Configuration

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

## Migration folder

```text
src/main/resources/db/migration
```

## V1 migration

```sql
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    completed BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

## Golden rule

```text
Hibernate maps entities.
Flyway manages database schema.
Never edit an old migration after it has already run.
Create a new migration for new changes.
```

---

# Lesson 10 — V2 Migration: Priority and Due Date

## Migration

```sql
ALTER TABLE tasks
ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE tasks
ADD COLUMN due_date DATE;
```

## Enum

```java
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}
```

## Entity fields

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private TaskPriority priority = TaskPriority.MEDIUM;

@Column(name = "due_date")
private LocalDate dueDate;
```

## Request validation

```java
@NotNull(message = "Priority is required")
private TaskPriority priority;

@FutureOrPresent(message = "Due date must be today or in the future")
private LocalDate dueDate;
```

## Golden rule

```text
Use EnumType.STRING, not ORDINAL.
Use LocalDate for date-only values.
Use new Flyway migration for schema changes.
```

---

# Lesson 11 — Mapper Pattern

## What you learned

You moved mapping logic out of the controller.

## `TaskMapper`

```java
@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.isCompleted(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public List<TaskResponse> toResponseList(List<Task> tasks) {
        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskPageResponse toPageResponse(Page<Task> taskPage) {
        List<TaskResponse> content = taskPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

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
```

## Golden rule

```text
Controller handles HTTP.
Service handles business logic.
Repository handles database.
Mapper converts entity to DTO.
```

---

# Lesson 12 — PUT vs PATCH

## What you learned

```text
PUT = full update
PATCH = partial update
```

## `PatchTaskRequest`

```java
public class PatchTaskRequest {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    private Boolean completed;

    private TaskPriority priority;

    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;
}
```

## Service method

```java
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
```

## Complete endpoint

```java
@PatchMapping("/{id}/complete")
public ResponseEntity<TaskResponse> completeTask(@PathVariable Long id) {
    Task task = taskService.completeTask(id);
    return ResponseEntity.ok(taskMapper.toResponse(task));
}
```

## Golden rule

```text
Use Boolean, not boolean, for optional PATCH fields.
null means the client did not send the field.
```

---

# Lesson 13 — JPQL Advanced Search

## What you learned

You learned custom JPQL search.

## Example query

```java
@Query("""
        SELECT t FROM Task t
        WHERE (:title IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%')))
          AND (:completed IS NULL OR t.completed = :completed)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:dueAfter IS NULL OR t.dueDate >= :dueAfter)
          AND (:dueBefore IS NULL OR t.dueDate <= :dueBefore)
        """)
Page<Task> searchTasks(
        @Param("title") String title,
        @Param("completed") Boolean completed,
        @Param("priority") TaskPriority priority,
        @Param("dueAfter") LocalDate dueAfter,
        @Param("dueBefore") LocalDate dueBefore,
        Pageable pageable
);
```

## Important rule

JPQL uses Java entity names and field names:

```text
Task
t.dueDate
t.createdAt
```

Not database names:

```text
tasks
due_date
created_at
```

---

# Lesson 14 — Specification Dynamic Search

## What you learned

Specification is better for many optional filters.

## Repository

```java
public interface TaskRepository
        extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
}
```

## `TaskSpecifications`

```java
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> withFilters(
            String title,
            Boolean completed,
            TaskPriority priority,
            LocalDate dueAfter,
            LocalDate dueBefore,
            LocalDate dueDate
    ) {
        return Specification
                .where(titleContains(title))
                .and(hasCompleted(completed))
                .and(hasPriority(priority))
                .and(dueDateAfterOrEqual(dueAfter))
                .and(dueDateBeforeOrEqual(dueBefore))
                .and(hasDueDate(dueDate));
    }

    private static Specification<Task> titleContains(String title) {
        return (root, query, criteriaBuilder) -> {
            if (title == null || title.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    "%" + title.toLowerCase() + "%"
            );
        };
    }

    private static Specification<Task> hasCompleted(Boolean completed) {
        return (root, query, criteriaBuilder) -> {
            if (completed == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("completed"), completed);
        };
    }

    private static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, criteriaBuilder) -> {
            if (priority == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("priority"), priority);
        };
    }

    private static Specification<Task> dueDateAfterOrEqual(LocalDate dueAfter) {
        return (root, query, criteriaBuilder) -> {
            if (dueAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueAfter);
        };
    }

    private static Specification<Task> dueDateBeforeOrEqual(LocalDate dueBefore) {
        return (root, query, criteriaBuilder) -> {
            if (dueBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueBefore);
        };
    }

    private static Specification<Task> hasDueDate(LocalDate dueDate) {
        return (root, query, criteriaBuilder) -> {
            if (dueDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dueDate"), dueDate);
        };
    }
}
```

## Golden rule

```text
Use derived query methods for simple queries.
Use JPQL for fixed custom queries.
Use Specification for many optional filters.
```

---

# Lesson 15 — Transactions

## What you learned

A transaction means all database operations succeed together or fail together.

## Class-level transaction

```java
@Service
@Transactional(readOnly = true)
public class TaskService {
}
```

## Write methods

```java
@Transactional
public Task createTask(CreateTaskRequest request) {
    ...
}
```

## Golden rule

```text
Controller handles HTTP.
Service handles business transaction.
Repository handles database.
```

---

# Lesson 16 — User and Task Relationship

## What you learned

```text
One user can have many tasks.
One task belongs to one user.
```

## Migration

```sql
CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE tasks
ADD COLUMN user_id BIGINT;

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_user
FOREIGN KEY (user_id)
REFERENCES app_users(id);
```

## `Task`

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private AppUser user;
```

## Golden rule

```text
Foreign key lives on the many side.
Task is the many side.
AppUser is the one side.
```

---

# Lesson 17 — Get Tasks by User

## Endpoints

```http
GET /api/users/{id}/tasks
GET /api/users/{id}/tasks/completed
```

## Repository

```java
Page<Task> findByUserId(Long userId, Pageable pageable);

Page<Task> findByUserIdAndCompleted(Long userId, boolean completed, Pageable pageable);
```

## Golden rule

```text
User exists but has no tasks → 200 OK with empty page.
User does not exist → error response.
```

---

# Lesson 18 — N+1 Query Problem and Fetch Join

## What you learned

N+1 means:

```text
1 query for main data
+ N queries for relationships
```

## Fetch join

```java
@Query(
        value = """
                SELECT t FROM Task t
                LEFT JOIN FETCH t.user
                """,
        countQuery = """
                SELECT COUNT(t) FROM Task t
                """
)
Page<Task> findAllWithUser(Pageable pageable);
```

## Golden rule

```text
Keep relationships LAZY.
Fetch only what the API needs.
Watch SQL logs.
```

---

# Lesson 19 — `@EntityGraph`

## What you learned

You replaced some fetch-join queries with `@EntityGraph`.

## Repository

```java
@EntityGraph(attributePaths = "user")
Page<Task> findAll(Pageable pageable);

@EntityGraph(attributePaths = "user")
Page<Task> findByCompleted(boolean completed, Pageable pageable);

@EntityGraph(attributePaths = "user")
Page<Task> findByUserId(Long userId, Pageable pageable);

@EntityGraph(attributePaths = "user")
Page<Task> findByUserIdAndCompleted(Long userId, boolean completed, Pageable pageable);
```

## Golden rule

```text
Use @EntityGraph for simple relationship fetching.
Use JOIN FETCH when you need custom JPQL.
attributePaths uses Java field names, not database column names.
```

---

# Lesson 20 — One-to-Many Safely

## `AppUser`

```java
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<Task> tasks = new ArrayList<>();
```

## Explanation

`mappedBy = "user"` points to this field in `Task`:

```java
private AppUser user;
```

## Golden rule

```text
mappedBy uses Java field name, not database column name.
Do not return entity relationship lists directly in API response.
```

---

# Lesson 21 — Cascade and Orphan Removal

## No cascade example

```java
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<Task> tasks = new ArrayList<>();
```

## Cascade example

```java
@OneToMany(
        mappedBy = "task",
        cascade = CascadeType.ALL,
        orphanRemoval = true
)
private List<TaskComment> comments = new ArrayList<>();
```

## Decisions

```text
User → Task: usually no cascade delete
Task → Comment: cascade delete may be okay
Project → Task: be careful
Order → OrderItem: cascade delete usually okay
```

## Golden rule

```text
If child is real history, avoid cascade delete.
If child cannot exist without parent, cascade/orphanRemoval may be appropriate.
```

---

# Lesson 22 — Task Comments

## Migration

```sql
CREATE TABLE task_comments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_task_comments_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
);
```

## `TaskComment`

```java
@Entity
@Table(name = "task_comments")
@EntityListeners(AuditingEntityListener.class)
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

## Endpoints

```http
POST   /api/tasks/{taskId}/comments
GET    /api/tasks/{taskId}/comments
PUT    /api/tasks/{taskId}/comments/{commentId}
DELETE /api/tasks/{taskId}/comments/{commentId}
```

## Golden rule

```text
TaskComment owns the foreign key task_id.
Task has the collection mappedBy = "task".
Use DTOs for comment responses.
```

---

# Lesson 23 — Professional Custom Exceptions

## Exception classes

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
```

```java
public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(Long id) {
        super("Comment not found with id: " + id);
    }
}
```

```java
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

```java
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```

```java
public class TaskAlreadyCompletedException extends BadRequestException {
    public TaskAlreadyCompletedException(Long id) {
        super("Task is already completed with id: " + id);
    }
}
```

## Status code rules

```text
Not found → 404
Duplicate resource → 409
Bad client action → 400
Validation error → 400
```

## Golden rule

```text
Service throws business exceptions.
GlobalExceptionHandler converts them into clean JSON.
Controller stays clean.
```

---

# Troubleshooting Notes

## Missing table

```text
Schema validation: missing table [app_user]
```

Cause:

```text
Entity expected app_user but migration created app_users.
```

Fix:

```java
@Table(name = "app_users")
```

## Missing column

```text
Schema validation: missing column [created_date]
```

Cause:

```text
Entity expected created_date but database has created_at.
```

Fix:

```java
@Column(name = "created_at")
private Instant createdAt;
```

## Circular dependency

```text
Requested bean is currently in creation
```

Common cause:

```text
UserService injects UserService
or Service A → Service B → Service A
```

Fix:

```text
UserService should inject UserRepository, not itself.
Avoid service dependency cycles.
```

## 405 Method Not Allowed

Cause:

```text
Wrong HTTP method.
```

Example wrong:

```text
POST /api/users/1/tasks
```

Correct:

```text
GET /api/users/1/tasks
```

---

# Final Step 2 Architecture

```text
Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL
```

Support layers:

```text
DTO             = request/response model
Mapper          = entity ↔ DTO conversion
Exception       = business errors
Flyway          = database schema migration
Specification   = dynamic query filters
EntityGraph     = relationship fetch planning
```

---

# Final Step 2 Golden Rules

```text
Use PostgreSQL for real persistence.
Use JPA entities for database mapping.
Use repositories for database access.
Use services for business logic and transactions.
Use DTOs for request/response.
Use mappers for conversion.
Use Flyway for database schema changes.
Use Specifications for dynamic search.
Use pagination for list endpoints.
Use sorting safely with whitelisted fields.
Use LAZY relationships by default.
Use EntityGraph or fetch join when response needs relationship data.
Do not expose entity graphs directly in API responses.
Do not use CascadeType.ALL blindly.
Use custom exceptions for business errors.
```

---

# Step 2 Final Status

You completed Step 2 for your current learning level.

You can now build a real database-backed REST API with:

```text
PostgreSQL
Spring Data JPA
Hibernate
Flyway
DTO
Validation
Mapper
Pagination
Sorting
Filtering
Relationships
Transactions
Clean error handling
```

---

# Next Phase

The next phase is:

```text
Phase 4 — Spring Security + Authentication
```

Recommended next lessons:

1. What is Spring Security?
2. Add `spring-boot-starter-security`
3. Understand default login behavior
4. Create register API
5. Hash passwords with BCrypt
6. Create login API
7. JWT access token
8. Protect endpoints
9. Role-based authorization
10. Current user endpoint
