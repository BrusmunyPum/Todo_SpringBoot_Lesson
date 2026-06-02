# Todo API — Complete Project Review
> Spring Boot 4.0.6 · Spring Security 7 · PostgreSQL · JJWT 0.12.6
> Review date: June 2026

---

## Table of Contents
1. [Project Structure](#1-project-structure)
2. [Architecture Overview](#2-architecture-overview)
3. [Database Schema](#3-database-schema)
4. [All API Endpoints](#4-all-api-endpoints)
5. [Request → Response Flow](#5-request--response-flow)
6. [Layer by Layer Explanation](#6-layer-by-layer-explanation)
7. [Every File Explained](#7-every-file-explained)
8. [What You Learned](#8-what-you-learned)
9. [Bugs Found & Fixed](#9-bugs-found--fixed)
10. [Known Issues to Fix Later](#10-known-issues-to-fix-later)
11. [Code Quality Score](#11-code-quality-score)

---

## 1. Project Structure

```
src/main/java/com/example/todo/
│
├── TodoApplication.java                    ← Entry point
├── HelloController.java                    ← ⚠️ DELETE THIS (leftover test file)
│
├── auth/                                   ← Authentication module
│   ├── controller/AuthController.java      ← register, login, logout
│   ├── service/
│   │   ├── AuthService.java                ← business logic
│   │   └── CustomUserDetailsService.java   ← Spring Security user loader
│   └── dto/
│       ├── request/LoginRequest.java
│       ├── request/RegisterRequest.java
│       ├── response/LoginResponse.java
│       └── response/RegisterResponse.java
│
├── task/                                   ← Task module
│   ├── controller/TaskController.java
│   ├── service/TaskService.java
│   ├── repository/
│   │   ├── TaskRepository.java
│   │   └── TaskSpecifications.java         ← Dynamic filtering
│   ├── entity/
│   │   ├── Task.java
│   │   └── TaskPriority.java               ← LOW, MEDIUM, HIGH
│   ├── mapper/TaskMapper.java
│   └── dto/
│       ├── request/CreateTaskRequest.java
│       ├── request/UpdateTaskRequest.java
│       ├── request/PatchTaskRequest.java
│       ├── response/TaskResponse.java
│       └── response/TaskPageResponse.java
│
├── user/                                   ← User module
│   ├── controller/UserController.java
│   ├── service/UserService.java
│   ├── repository/UserRepository.java
│   ├── entity/
│   │   ├── AppUser.java                    ← implements UserDetails
│   │   └── UserRole.java                   ← USER, ADMIN
│   ├── mapper/UserMapper.java
│   └── dto/response/
│       ├── UserResponse.java
│       └── UserDetailResponse.java
│
├── comment/                                ← Comment module
│   ├── controller/TaskCommentController.java
│   ├── service/TaskCommentService.java
│   ├── repository/TaskCommentRepository.java
│   ├── entity/TaskComment.java
│   ├── mapper/TaskCommentMapper.java
│   └── dto/
│       ├── request/CreateTaskCommentRequest.java
│       └── response/TaskCommentResponse.java
│
└── common/                                 ← Shared across all modules
    ├── entity/BaseEntity.java              ← createdAt, updatedAt
    ├── security/
    │   ├── SecurityConfig.java             ← Full security setup + CORS
    │   ├── JwtService.java                 ← Generate + validate tokens
    │   ├── JwtAuthFilter.java              ← Intercepts every request
    │   └── CustomAuthenticationEntryPoint.java ← 401 JSON response
    └── exception/
        ├── GlobalExceptionHandler.java     ← Handles all exceptions → JSON
        ├── ApiError.java                   ← Standard error response shape
        ├── BadRequestException.java
        ├── CommentNotFoundException.java
        ├── DuplicateResourceException.java
        ├── InvalidCredentialsException.java
        ├── TaskAlreadyCompletedException.java
        ├── TaskNotFoundException.java
        └── UserNotFoundException.java

src/main/resources/
├── application.properties
└── db/migration/
    ├── V1__create_tasks_table.sql
    ├── V2__add_priority_and_due_date_to_tasks.sql
    ├── V3__create_users_and_assign_tasks.sql
    ├── V4__create_task_comments_table.sql
    ├── V5__add_password_to_users.sql
    └── V6__add_role_to_users.sql
```

---

## 2. Architecture Overview

### Layer Architecture
```
HTTP Request
     │
     ▼
┌─────────────────────────────────┐
│   Spring Security Filter Chain  │  JwtAuthFilter → AuthorizationFilter
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│         Controller Layer        │  @RestController
│   TaskController                │  Receives HTTP, returns ResponseEntity
│   UserController                │  Uses DTOs — never exposes entities
│   AuthController                │
│   TaskCommentController         │
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│          Service Layer          │  @Service
│   TaskService                   │  Business logic lives here
│   UserService                   │  @Transactional(readOnly = true)
│   AuthService                   │  @Transactional on write methods
│   TaskCommentService            │
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│        Repository Layer         │  @Repository (JpaRepository)
│   TaskRepository                │  Spring Data JPA generates SQL
│   UserRepository                │  Named queries + @EntityGraph
│   TaskCommentRepository         │
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│      PostgreSQL Database        │  Managed by Flyway migrations
└─────────────────────────────────┘
```

### Key Design Decisions

| Decision | Why |
|---|---|
| Feature-based packages (task/, user/) | Easier to find related code, scales better |
| DTOs (not exposing entities) | Protects internal model, controls what client sees |
| BaseEntity | All entities automatically get createdAt + updatedAt |
| Flyway | Database changes are tracked and versioned |
| @EntityGraph | Solves N+1 query problem for Task → User relationship |
| Specification pattern | Dynamic filtering without writing multiple queries |
| Constructor injection | No hidden dependencies, easier to test |
| @Transactional(readOnly=true) | Better DB performance for read operations |

---

## 3. Database Schema

```sql
┌──────────────────────────────────────┐
│              app_users               │
├──────────────┬───────────────────────┤
│ id           │ BIGSERIAL PRIMARY KEY  │
│ username     │ VARCHAR(50) UNIQUE     │
│ email        │ VARCHAR(120) UNIQUE    │
│ password     │ VARCHAR(255)           │ ← BCrypt hash
│ role         │ VARCHAR(20)            │ ← 'USER' or 'ADMIN'
│ created_at   │ TIMESTAMPTZ            │
│ updated_at   │ TIMESTAMPTZ            │
└──────────────┴───────────────────────┘
        │
        │ ONE user has MANY tasks
        │
        ▼
┌──────────────────────────────────────┐
│                tasks                 │
├──────────────┬───────────────────────┤
│ id           │ BIGSERIAL PRIMARY KEY  │
│ title        │ VARCHAR(100)           │
│ completed    │ BOOLEAN                │
│ priority     │ VARCHAR(20)            │ ← 'LOW','MEDIUM','HIGH'
│ due_date     │ DATE                   │
│ user_id      │ BIGINT FK → app_users  │
│ created_at   │ TIMESTAMPTZ            │
│ updated_at   │ TIMESTAMPTZ            │
└──────────────┴───────────────────────┘
        │
        │ ONE task has MANY comments
        │
        ▼
┌──────────────────────────────────────┐
│           task_comments              │
├──────────────┬───────────────────────┤
│ id           │ BIGSERIAL PRIMARY KEY  │
│ task_id      │ BIGINT FK → tasks      │
│ content      │ VARCHAR(500)           │
│ created_at   │ TIMESTAMPTZ            │
│ updated_at   │ TIMESTAMPTZ            │
└──────────────┴───────────────────────┘
```

### Entity Relationships
```
AppUser ──── (1:N) ──── Task ──── (1:N) ──── TaskComment
```
- One user can have **many tasks**
- One task can have **many comments**
- A comment belongs to exactly **one task**

---

## 4. All API Endpoints

### 🔐 Auth — `/api/v1/auth/**` (public, no token needed)

| Method | URL | Request Body | Response | Status |
|---|---|---|---|---|
| POST | `/register` | `{username, email, password}` | `{id, username, email}` | 201 |
| POST | `/login` | `{username, password}` | `{accessToken, tokenType, expiresIn}` | 200 |
| POST | `/logout` | — | — | 204 |

### 👤 Users — `/api/v1/users/**` (token required)

| Method | URL | Auth | Response | Status |
|---|---|---|---|---|
| GET | `/me` | USER | My profile | 200 |
| GET | `/me/detail` | USER | My profile + taskCount | 200 |
| GET | `/me/tasks` | USER | My tasks (paginated) | 200 |
| GET | `/me/tasks/completed` | USER | My completed tasks | 200 |
| GET | `/` | **ADMIN** | All users | 200 |
| GET | `/{id}` | **ADMIN** | User by ID | 200 |
| GET | `/{id}/detail` | **ADMIN** | User detail by ID | 200 |
| GET | `/{id}/tasks` | **ADMIN** | Tasks by user ID | 200 |
| GET | `/{id}/tasks/completed` | **ADMIN** | Completed tasks by user ID | 200 |

### ✅ Tasks — `/api/v1/tasks/**` (token required)

| Method | URL | Description | Status |
|---|---|---|---|
| GET | `/` | All tasks (paginated, filterable) | 200 |
| GET | `/search` | Search tasks (title, priority, date, etc.) | 200 |
| GET | `/{id}` | Get task by ID | 200 |
| POST | `/` | Create task | 201 |
| PUT | `/{id}` | Full update (all fields required) | 200 |
| PATCH | `/{id}` | Partial update (only send fields to change) | 200 |
| PATCH | `/{id}/complete` | Mark as completed | 200 |
| PATCH | `/{id}/reopen` | Reopen a completed task | 200 |
| DELETE | `/{id}` | Delete task | 204 |

### Query parameters for GET /tasks and /search

| Param | Type | Default | Example |
|---|---|---|---|
| `page` | int | 0 | `?page=0` |
| `size` | int | 5 | `?size=10` |
| `sortBy` | string | id | `?sortBy=dueDate` |
| `direction` | string | asc | `?direction=desc` |
| `completed` | boolean | — | `?completed=false` |
| `title` | string | — | `?title=buy` |
| `priority` | enum | — | `?priority=HIGH` |
| `dueAfter` | date | — | `?dueAfter=2026-01-01` |
| `dueBefore` | date | — | `?dueBefore=2026-12-31` |

### 💬 Comments — `/api/v1/tasks/{taskId}/comments/**` (token required)

| Method | URL | Description | Status |
|---|---|---|---|
| GET | `/` | Get all comments for a task | 200 |
| POST | `/` | Add a comment | 201 |
| PUT | `/{commentId}` | Update a comment | 200 |
| DELETE | `/{commentId}` | Delete a comment | 204 |

---

## 5. Request → Response Flow

### Normal request with JWT
```
Postman/Browser
     │
     │ GET /api/v1/tasks
     │ Authorization: Bearer eyJ...
     │
     ▼
JwtAuthFilter
     │ Extract token from header
     │ Extract username from token
     │ Load AppUser from DB
     │ Validate token (signature + expiry)
     │ Set SecurityContextHolder
     │
     ▼
AuthorizationFilter
     │ Check: anyRequest().authenticated() → user IS authenticated → PASS
     │
     ▼
TaskController.getAllTasks()
     │ Calls TaskService.getAllTasks()
     │
     ▼
TaskService.getAllTasks()
     │ Builds Pageable
     │ Calls TaskRepository.findAll(pageable)
     │
     ▼
TaskRepository
     │ Generates SQL with @EntityGraph (joins user)
     │ Returns Page<Task>
     │
     ▼
TaskMapper.toPageResponse()
     │ Converts Task entities → TaskResponse DTOs
     │ Wraps in TaskPageResponse
     │
     ▼
ResponseEntity.ok(taskPageResponse)
     │ 200 OK with JSON body
     │
     ▼
Browser/Postman
```

### Login flow
```
POST /api/v1/auth/login
{ username, password }
     │
     ▼
JwtAuthFilter → no token → skip
     │
     ▼
AuthorizationFilter → permitAll() → PASS
     │
     ▼
AuthController.login()
     │
     ▼
AuthService.login()
     │ userRepository.findByUsername()
     │   → not found → InvalidCredentialsException → 401
     │ passwordEncoder.matches(raw, hash)
     │   → no match → InvalidCredentialsException → 401
     │ jwtService.generateToken(user)
     │ return LoginResponse
     │
     ▼
200 OK { accessToken, tokenType, expiresIn }
```

### Error flow — No token
```
GET /api/v1/tasks (no Authorization header)
     │
     ▼
JwtAuthFilter → no header → skip
     │
     ▼
AuthorizationFilter
     │ anyRequest().authenticated() → user NOT authenticated → FAIL
     │
     ▼
ExceptionTranslationFilter
     │ Calls CustomAuthenticationEntryPoint
     │
     ▼
401 { "status": 401, "message": "Authentication required..." }
```

### Error flow — Wrong role
```
GET /api/v1/users (with ROLE_USER token)
     │
     ▼
JwtAuthFilter → valid token → sets ROLE_USER in SecurityContext
     │
     ▼
AuthorizationFilter → authenticated → PASS
     │
     ▼
UserController.getAllUsers()
     │ @PreAuthorize("hasRole('ADMIN')") → FAIL (user is ROLE_USER)
     │ throws AccessDeniedException
     │
     ▼
GlobalExceptionHandler.handleAccessDeniedException()
     │
     ▼
403 { "status": 403, "message": "You do not have permission..." }
```

---

## 6. Layer by Layer Explanation

### Controller Layer
- **Purpose:** Receive HTTP request, call service, return response
- **Rule:** No business logic here — only call service + map to DTO
- **Annotations:** `@RestController`, `@RequestMapping`, `@GetMapping`, etc.
- **Returns:** Always `ResponseEntity<T>`

```java
@PostMapping
public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
    Task task = taskService.createTask(request);       // delegate to service
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskMapper.toResponse(task));        // map entity to DTO
}
```

### Service Layer
- **Purpose:** Business logic — validation, rules, orchestration
- **Rule:** All business decisions made here, not in controller or repository
- **Annotations:** `@Service`, `@Transactional(readOnly = true)`
- **Write methods:** `@Transactional` (overrides class-level readOnly)

```java
@Service
@Transactional(readOnly = true)   // all methods are read-only by default
public class TaskService {

    @Transactional                // override for write operation
    public Task createTask(CreateTaskRequest request) { ... }

    public Task getTaskById(Long id) { ... }   // inherits readOnly = true
}
```

### Repository Layer
- **Purpose:** Data access — talking to the database
- **Rule:** No business logic — only queries
- **Extends:** `JpaRepository<Entity, ID>` → gives you free CRUD
- **Named queries:** Spring generates SQL from method names

```java
// Spring generates: SELECT * FROM tasks WHERE user_id = ? AND completed = ?
Page<Task> findByUserIdAndCompleted(Long userId, boolean completed, Pageable pageable);
```

### Entity Layer
- **Purpose:** Maps Java class to database table
- **Rule:** No business logic — just fields, relationships, and JPA annotations
- **Important:** Always has `protected Entity() {}` for JPA

```java
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity { ... }
```

### DTO Layer
- **Purpose:** Transfer data between client and server
- **Rule:** Never expose entities directly to client — always use DTOs
- **Request DTOs:** What client sends to server (with validation)
- **Response DTOs:** What server sends to client

```
Client sends:  CreateTaskRequest { title, priority, dueDate, userId }
Server stores: Task entity { id, title, completed, priority, dueDate, user, createdAt, updatedAt }
Client gets:   TaskResponse { id, title, completed, priority, dueDate, userId, username, createdAt, updatedAt }
```

### Mapper Layer
- **Purpose:** Convert between Entity ↔ DTO
- **Rule:** No business logic — just field mapping
- **Annotation:** `@Component`

---

## 7. Every File Explained

### Entry Point
| File | Purpose |
|---|---|
| `TodoApplication.java` | Starts Spring Boot. `@EnableJpaAuditing` enables `createdAt`/`updatedAt` auto-fill |

### Common / Shared
| File | Purpose |
|---|---|
| `BaseEntity.java` | Abstract class extended by all entities. Provides `createdAt` and `updatedAt` with JPA Auditing |
| `ApiError.java` | Standard error response shape: `{status, message, errors, timestamp}` |
| `GlobalExceptionHandler.java` | `@RestControllerAdvice` — catches ALL exceptions and returns clean JSON |
| `SecurityConfig.java` | Full Spring Security setup: filter chain, CORS, BCrypt, JWT filter |
| `JwtService.java` | Generates JWT tokens (login) and validates them (every request) |
| `JwtAuthFilter.java` | Runs on every request — reads Authorization header, validates token, sets SecurityContext |
| `CustomAuthenticationEntryPoint.java` | Returns `401` JSON when unauthenticated request hits protected endpoint |

### Exception Classes

| Exception | Extends | HTTP Code | When thrown |
|---|---|---|---|
| `TaskNotFoundException` | `RuntimeException` | 404 | Task ID not in DB |
| `UserNotFoundException` | `RuntimeException` | 404 | User ID not in DB |
| `CommentNotFoundException` | `RuntimeException` | 404 | Comment ID not in DB |
| `DuplicateResourceException` | `RuntimeException` | 409 | Duplicate username or email |
| `BadRequestException` | `RuntimeException` | 400 | Invalid business request |
| `TaskAlreadyCompletedException` | `BadRequestException` | 409 | Completing already completed task |
| `InvalidCredentialsException` | `RuntimeException` | 401 | Wrong username or password |

### Auth Module
| File | Purpose |
|---|---|
| `AuthController.java` | `POST /register`, `POST /login`, `POST /logout` |
| `AuthService.java` | Register (hash + save), Login (verify + generate token) |
| `CustomUserDetailsService.java` | Spring Security calls this to load user from DB during token validation |
| `LoginRequest.java` | `{username, password}` with `@NotBlank` validation |
| `RegisterRequest.java` | `{username, email, password}` with full validation |
| `LoginResponse.java` | `{accessToken, tokenType, expiresIn}` |
| `RegisterResponse.java` | `{id, username, email}` |

### Task Module
| File | Purpose |
|---|---|
| `TaskController.java` | All task CRUD endpoints |
| `TaskService.java` | CRUD logic, pagination, sorting, complete/reopen |
| `TaskRepository.java` | JPA queries with `@EntityGraph` to avoid N+1 |
| `TaskSpecifications.java` | Dynamic filter building for `/search` endpoint |
| `Task.java` | Entity: id, title, completed, priority, dueDate, user, comments |
| `TaskPriority.java` | Enum: `LOW`, `MEDIUM`, `HIGH` |
| `TaskMapper.java` | `Task → TaskResponse`, `Page<Task> → TaskPageResponse` |
| `CreateTaskRequest.java` | title, priority, dueDate, userId (all validated) |
| `UpdateTaskRequest.java` | Same fields but title+priority required (full replace) |
| `PatchTaskRequest.java` | All fields optional (partial update) |
| `TaskResponse.java` | Full task data including userId and username |
| `TaskPageResponse.java` | content + pagination metadata (page, size, totalElements, totalPages) |

### User Module
| File | Purpose |
|---|---|
| `UserController.java` | `/me` endpoints (any user) + admin endpoints (`@PreAuthorize`) |
| `UserService.java` | Get user, get all users, get user detail with task count |
| `UserRepository.java` | `findByUsername`, `existsByUsername`, `existsByEmail` |
| `AppUser.java` | Entity + `UserDetails`. Fields: id, username, email, password, role, tasks |
| `UserRole.java` | Enum: `USER`, `ADMIN` |
| `UserMapper.java` | `AppUser → UserResponse` |
| `UserResponse.java` | `{id, username, email, createdAt, updatedAt}` |
| `UserDetailResponse.java` | `{id, username, email, taskCount, createdAt, updatedAt}` |

### Comment Module
| File | Purpose |
|---|---|
| `TaskCommentController.java` | GET/POST/PUT/DELETE comments for a task |
| `TaskCommentService.java` | Comment CRUD + validates comment belongs to correct task |
| `TaskCommentRepository.java` | `findByTaskIdOrderByCreatedAtDesc` |
| `TaskComment.java` | Entity: id, content, task (ManyToOne) |
| `TaskCommentMapper.java` | `TaskComment → TaskCommentResponse` |
| `CreateTaskCommentRequest.java` | `{content}` with `@NotBlank @Size(max=500)` |
| `TaskCommentResponse.java` | `{id, taskId, content, createdAt, updatedAt}` |

---

## 8. What You Learned

### Phase 1–3: Foundation + REST API
- ✅ Spring Boot project structure and Maven
- ✅ `application.properties` configuration
- ✅ Dependency Injection (constructor injection)
- ✅ IoC container — Spring manages your objects
- ✅ Controller → Service → Repository → Entity layering
- ✅ DTOs (Request/Response separation from entities)
- ✅ `@RestController`, `@Service`, `@Repository`, `@Component`
- ✅ REST API (GET, POST, PUT, PATCH, DELETE)
- ✅ `ResponseEntity` with proper HTTP status codes
- ✅ `@Valid`, `@NotBlank`, `@Size`, `@Email`, `@FutureOrPresent`
- ✅ Global exception handling with `@RestControllerAdvice`
- ✅ Custom exception classes
- ✅ Pagination, sorting, filtering with Spring Data JPA
- ✅ Mapper pattern

### Phase 2: PostgreSQL + JPA
- ✅ PostgreSQL connection with HikariCP connection pool
- ✅ JPA entities with `@Entity`, `@Table`, `@Column`
- ✅ Primary key with `@Id` + `@GeneratedValue(IDENTITY)`
- ✅ `@ManyToOne`, `@OneToMany` relationships
- ✅ `FetchType.LAZY` vs eager loading
- ✅ `@EntityGraph` to solve N+1 problem
- ✅ `@Enumerated(EnumType.STRING)` for enum columns
- ✅ JPA Auditing with `@CreatedDate`, `@LastModifiedDate`
- ✅ `BaseEntity` with `@MappedSuperclass`
- ✅ Flyway database migrations
- ✅ Spring Data JPA `JpaRepository`
- ✅ Named query methods
- ✅ `JpaSpecificationExecutor` + Specification pattern
- ✅ `@Transactional`, `readOnly = true`
- ✅ `PageRequest`, `Pageable`, `Page<T>`

### Phase 4: Security + JWT
- ✅ Spring Security filter chain
- ✅ BCrypt password hashing
- ✅ JWT generation and validation (JJWT 0.12.6)
- ✅ `JwtAuthFilter` extending `OncePerRequestFilter`
- ✅ `SecurityContextHolder` and `Authentication`
- ✅ `AppUser implements UserDetails`
- ✅ `CustomUserDetailsService implements UserDetailsService`
- ✅ `DaoAuthenticationProvider` (Spring Security 7 constructor style)
- ✅ Stateless session management
- ✅ Role-based authorization with `@PreAuthorize`
- ✅ `@EnableMethodSecurity`
- ✅ `@AuthenticationPrincipal` for current user
- ✅ CORS configuration
- ✅ `CustomAuthenticationEntryPoint`
- ✅ `GlobalExceptionHandler` for security exceptions

---

## 9. Bugs Found & Fixed

### Bug 1 — `TaskPageResponse` missing `getTotalPages()` ✅ FIXED
**Problem:** `totalPages` field had no getter — JSON response never included `totalPages`
```java
// Before — missing getter
private int totalPages;  // never serialized!

// After — getter added
public int getTotalPages() { return totalPages; }
```

### Bug 2 — `HelloController` leftover file ⚠️ PARTIALLY FIXED
**Problem:** Had 3 unsecured endpoints (`/`, `/hello`, `/api/hello`) with no authentication
**Fix applied:** Reduced to 1 safe endpoint. **You must manually delete this file in IntelliJ.**
```
Right-click HelloController.java → Delete
```

### Bug 3 — Commented-out code in `TaskService` ✅ FIXED
**Problem:** Old `completeTask()` method was commented out — messy and confusing
```java
// Removed:
//    @Transactional
//    public Task completeTask(Long id) { ... old version ... }
```

### Bug 4 — `Task` constructor on one line ✅ FIXED
```java
// Before (hard to read)
public Task(String title, ...) {
    this.title = title; this.completed = completed; this.priority = priority; ...
}

// After (clean)
public Task(String title, ...) {
    this.title = title;
    this.completed = completed;
    this.priority = priority;
    this.dueDate = dueDate;
    this.user = user;
}
```

---

## 10. Known Issues to Fix Later

### Issue 1 — Hardcoded credentials in `application.properties` ⚠️
**Fix in:** Phase 9 (Docker) using environment variables
```properties
# Current (unsafe)
spring.datasource.password=Muny168168
jwt.secret=CdJeCU+...

# Future (safe)
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

### Issue 2 — Any user can update/delete any task 🔒
**Problem:** No ownership check. User A can delete User B's tasks.
**Fix:** Check `task.getUser().getId().equals(currentUser.getId())` in service
**Fix in:** When adding full user isolation (future feature)

### Issue 3 — Any user can delete any comment 🔒
**Same problem as Issue 2** — no comment ownership check on delete
**Fix in:** Same time as Issue 2

### Issue 4 — `CreateTaskRequest` accepts `userId` from client ⚠️
**Problem:** User can create tasks for any user by sending any `userId`
**Better approach:** Get userId from `@AuthenticationPrincipal` instead
**Fix in:** When implementing full user isolation

### Issue 5 — `CreateTaskCommentRequest` used for both create and update
**Minor:** Should have a separate `UpdateTaskCommentRequest` for clarity
**Fix:** Low priority, rename when time allows

---

## 11. Code Quality Score

| Category | Score | Notes |
|---|---|---|
| **Package Structure** | ⭐⭐⭐⭐⭐ | Feature-based, clean |
| **Layering** | ⭐⭐⭐⭐⭐ | Correct Controller → Service → Repository |
| **DTOs** | ⭐⭐⭐⭐☆ | Good separation, minor reuse issue in comments |
| **Validation** | ⭐⭐⭐⭐⭐ | All inputs validated |
| **Error Handling** | ⭐⭐⭐⭐⭐ | Clean JSON errors for all cases |
| **Security** | ⭐⭐⭐⭐☆ | JWT correct, no ownership checks yet |
| **Database** | ⭐⭐⭐⭐⭐ | Flyway migrations, N+1 solved, transactions correct |
| **Code Cleanliness** | ⭐⭐⭐⭐☆ | Minor issues fixed in review |
| **Testing** | ⭐☆☆☆☆ | No tests yet — Phase 5 |
| **Documentation** | ⭐☆☆☆☆ | No Swagger yet — Phase 7 |

**Overall: 38/50 — Solid intermediate-level backend** 🎯

---

## Quick Cheat Sheet

### How to add a new feature (e.g., Projects)

1. **Migration** → `V7__create_projects_table.sql`
2. **Entity** → `project/entity/Project.java`
3. **Repository** → `project/repository/ProjectRepository.java`
4. **Request DTO** → `project/dto/request/CreateProjectRequest.java`
5. **Response DTO** → `project/dto/response/ProjectResponse.java`
6. **Mapper** → `project/mapper/ProjectMapper.java`
7. **Service** → `project/service/ProjectService.java`
8. **Controller** → `project/controller/ProjectController.java`

### Common annotations quick reference

| Annotation | Layer | Meaning |
|---|---|---|
| `@RestController` | Controller | HTTP handler that returns JSON |
| `@RequestMapping` | Controller | Base URL for all methods in class |
| `@GetMapping` / `@PostMapping` etc. | Controller | HTTP method + path |
| `@PathVariable` | Controller | `/{id}` URL segment |
| `@RequestParam` | Controller | `?page=0` query parameter |
| `@RequestBody` | Controller | JSON body parsed to object |
| `@Valid` | Controller | Triggers validation on request object |
| `@AuthenticationPrincipal` | Controller | Inject current logged-in user |
| `@PreAuthorize` | Controller | Check role before method runs |
| `@Service` | Service | Business logic bean |
| `@Transactional` | Service | DB operation in one transaction |
| `@Repository` | Repository | Data access bean (auto on JpaRepository) |
| `@Entity` | Entity | Maps class to DB table |
| `@Table` | Entity | Specify table name |
| `@Column` | Entity | Column constraints |
| `@Id` | Entity | Primary key |
| `@GeneratedValue` | Entity | Auto-increment |
| `@ManyToOne` | Entity | Foreign key relationship |
| `@OneToMany` | Entity | Reverse side of relationship |
| `@JoinColumn` | Entity | Specify FK column name |
| `@Enumerated` | Entity | Store enum as string in DB |
| `@Component` | Any | Generic Spring-managed bean |
| `@Configuration` | Config | Bean factory class |
| `@Bean` | Config | Declare a Spring bean |
| `@Value` | Any | Inject from application.properties |

---

*Review complete. Ready for Phase 5: Testing* ✅
