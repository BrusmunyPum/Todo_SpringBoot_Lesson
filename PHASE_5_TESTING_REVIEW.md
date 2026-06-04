# Phase 5: Testing — Complete Review Guide

> Spring Boot 4.x.x | Java 25 | JUnit 5 | Mockito 5 | AssertJ
> Project: Task Management API

---

## Table of Contents

1. [Why We Test](#1-why-we-test)
2. [The Three Types of Tests](#2-the-three-types-of-tests)
3. [Test Folder Structure](#3-test-folder-structure)
4. [JUnit 5 Basics](#4-junit-5-basics)
5. [AssertJ — Modern Assertions](#5-assertj--modern-assertions)
6. [Mockito — Mocking Dependencies](#6-mockito--mocking-dependencies)
7. [Service Layer Testing](#7-service-layer-testing)
8. [Repository Layer Testing](#8-repository-layer-testing)
9. [Controller Layer Testing with MockMvc](#9-controller-layer-testing-with-mockmvc)
10. [Integration Testing](#10-integration-testing)
11. [Spring Boot 4.x Testing Notes](#11-spring-boot-4x-testing-notes)
12. [Real Bugs Found by Tests](#12-real-bugs-found-by-tests)
13. [Quick Reference Card](#13-quick-reference-card)
14. [Common Mistakes Master List](#14-common-mistakes-master-list)

---

## 1. Why We Test

Tests are small programs that automatically check whether your code does the right thing.

**Without tests:** You open Postman and test manually every time you change something.
**With tests:** You run one command and know in seconds if everything still works.

```bash
./mvnw test
# → Tests run: 84, Failures: 0, Errors: 0
```

**Three reasons every professional developer writes tests:**

1. **Catch regressions** — When you add a new feature, tests tell you if you broke something old.
2. **Prove correctness** — Tests are proof that your code does what you say it does.
3. **Enable refactoring** — You can safely change code internals if tests still pass.

---

## 2. The Three Types of Tests

```
        /\
       /  \        End-to-End Tests
      /    \       (slow, test full system like a user)
     /──────\
    /        \     Integration Tests
   /          \    (medium, test multiple layers together)
  /────────────\
 /              \  Unit Tests
/________________\ (fast, test one class in isolation)
```

| Type | Speed | Database | Network | Mocks |
|---|---|---|---|---|
| Unit | Very fast (~ms) | No | No | Yes — all dependencies |
| Repository | Medium (~1s) | Real | No | No |
| Controller | Medium (~1s) | No | No | Services mocked |
| Integration | Slow (~3-10s) | Real | No | Nothing |

**Rule of thumb:** Write many unit tests, some repository/controller tests, few integration tests.

---

## 3. Test Folder Structure

```
src/
├── main/java/com/example/todo/
│   ├── auth/service/AuthService.java
│   └── task/service/TaskService.java
└── test/java/com/example/todo/          ← mirrors main structure
    ├── auth/
    │   ├── controller/AuthControllerTest.java
    │   └── service/AuthServiceTest.java
    ├── common/
    │   ├── TestJwtHelper.java
    │   └── security/JwtServiceTest.java
    ├── integration/
    │   └── TaskIntegrationTest.java
    ├── task/
    │   ├── controller/TaskControllerTest.java
    │   ├── repository/TaskRepositoryTest.java
    │   └── service/TaskServiceTest.java
    └── TodoApplicationTests.java
```

**Naming rules:**
- Test class = Original class + `Test` → `TaskService` → `TaskServiceTest`
- Test method names describe behavior: `shouldThrowWhenUsernameIsDuplicate()`
- Test resources: `src/test/resources/application-test.properties`

---

## 4. JUnit 5 Basics

### Dependencies

Spring Boot 4.x includes JUnit 5 via:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Key Annotations

| Annotation | What it does |
|---|---|
| `@Test` | Marks a method as a test |
| `@BeforeEach` | Runs before EACH test method |
| `@AfterEach` | Runs after EACH test method |
| `@BeforeAll` | Runs once before ALL tests (must be `static`) |
| `@DisplayName("...")` | Human-readable test name in reports |
| `@Nested` | Groups related tests inside a class |
| `@ParameterizedTest` | Run same test with multiple inputs |

### The AAA Pattern — Every Test Follows This

```java
@Test
@DisplayName("should register successfully when username and email are unique")
void shouldRegisterSuccessfully() {
    // ── Arrange ── Set up test data and mocks
    RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "pass123");
    when(userRepository.existsByUsername("muny")).thenReturn(false);

    // ── Act ── Call the method being tested
    AppUser result = authService.register(request);

    // ── Assert ── Check the result
    assertThat(result.getUsername()).isEqualTo("muny");
}
```

### `@Nested` — Grouping Tests

```java
class AuthServiceTest {

    @Nested
    @DisplayName("register()")
    class Register {
        @Test void shouldRegisterSuccessfully() { ... }
        @Test void shouldThrowWhenUsernameIsDuplicate() { ... }
    }

    @Nested
    @DisplayName("login()")
    class Login {
        @Test void shouldReturnTokenOnValidLogin() { ... }
        @Test void shouldThrowWhenPasswordIsWrong() { ... }
    }
}
```

Output in IDE/report:
```
AuthServiceTest
  register()
    ✅ should register successfully when username and email are unique
    ✅ should throw DuplicateResourceException when username already exists
  login()
    ✅ should return token on valid login
    ✅ should throw when password is wrong
```

---

## 5. AssertJ — Modern Assertions

AssertJ is the preferred assertion library. It's more readable than JUnit's built-in `assertEquals`.

```java
// ❌ Old JUnit style
assertEquals("muny", result.getUsername());
assertTrue(result.isCompleted());
assertThrows(Exception.class, () -> service.method());

// ✅ Modern AssertJ style
assertThat(result.getUsername()).isEqualTo("muny");
assertThat(result.isCompleted()).isTrue();
assertThatThrownBy(() -> service.method()).isInstanceOf(Exception.class);
```

### Common AssertJ Methods

```java
// Basic
assertThat(value).isEqualTo(expected);
assertThat(value).isNotNull();
assertThat(value).isNull();
assertThat(value).isTrue();
assertThat(value).isFalse();

// Strings
assertThat(str).isEqualTo("hello");
assertThat(str).isNotBlank();
assertThat(str).contains("jwt");
assertThat(str).doesNotContain("password");
assertThat(str).hasSize(3);    // JWT has 3 parts split by "."

// Numbers
assertThat(num).isPositive();
assertThat(num).isGreaterThan(0);
assertThat(num).isEqualTo(3L);
assertThat(num).isZero();

// Collections
assertThat(list).hasSize(3);
assertThat(list).isEmpty();
assertThat(list).isNotEmpty();
assertThat(list).allMatch(item -> !item.isCompleted());
assertThat(list).contains(element);

// Optional
assertThat(optional).isPresent();
assertThat(optional).isEmpty();
assertThat(optional).get().matches(task -> task.isCompleted());

// Exceptions
assertThatThrownBy(() -> service.method(arg))
    .isInstanceOf(TaskNotFoundException.class)
    .hasMessage("Task not found with id: 99")
    .hasMessageContaining("99");
```

---

## 6. Mockito — Mocking Dependencies

### What is a Mock?

A mock is a fake object that replaces a real dependency. You control exactly what it returns.

```
Real unit test:
  AuthService (REAL) → UserRepository (FAKE/MOCK)
                     → PasswordEncoder (FAKE/MOCK)
                     → JwtService (FAKE/MOCK)
```

### Setup — Annotations

```java
// Step 1: Activate Mockito
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Step 2: Create mocks automatically
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    // Step 3: Create real object, inject mocks into it
    @InjectMocks
    private AuthService authService;

    // Step 4: Capture arguments passed to mocks
    @Captor
    private ArgumentCaptor<AppUser> userCaptor;
}
```

### Stubbing — Telling Mocks What to Return

```java
// Return a value
when(userRepository.existsByUsername("muny")).thenReturn(false);

// Return based on any input
when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

// Return the same object that was passed in (simulate save)
when(userRepository.save(any(AppUser.class)))
    .thenAnswer(invocation -> invocation.getArgument(0));

// Throw an exception
when(userRepository.findByUsername("nobody"))
    .thenThrow(new UsernameNotFoundException("not found"));

// For void methods — throw
doThrow(new TaskNotFoundException(99L))
    .when(taskService).deleteTask(99L);
```

### Verifying — Checking What Was Called

```java
// Called exactly once
verify(userRepository, times(1)).save(any(AppUser.class));

// Never called
verify(userRepository, never()).save(any());

// Called at least once
verify(userRepository, atLeastOnce()).existsByUsername(anyString());

// Called with exact argument
verify(jwtService, times(1)).generateToken(eq(user));
```

### Argument Matchers

```java
any()                  // any non-null object
any(AppUser.class)     // any AppUser
anyString()            // any String
anyLong()              // any long
eq("exact")            // exactly this value (uses .equals())
eq(user)               // exactly this object
isNull()               // null value
argThat(x -> x.getId() > 0)  // custom condition
```

**Important rule:** If you use any matcher in a method call, ALL arguments must use matchers.
```java
// ❌ WRONG — mixing literal and matcher
when(repo.findByIdAndUser(1L, eq(user))).thenReturn(...);

// ✅ CORRECT — all matchers
when(repo.findByIdAndUser(eq(1L), eq(user))).thenReturn(...);
```

### ArgumentCaptor — Inspect What Was Passed

```java
@Captor
private ArgumentCaptor<AppUser> userCaptor;

// After act:
authService.register(request);

// Capture what was passed to save()
verify(userRepository).save(userCaptor.capture());
AppUser captured = userCaptor.getValue();

// Now assert on the captured object
assertThat(captured.getPassword()).isEqualTo("$2a$hashed");
assertThat(captured.getPassword()).doesNotContain("password123"); // never plain text!
assertThat(captured.getRole()).isEqualTo(UserRole.USER);
```

### `@Value` Fields in Unit Tests

`@InjectMocks` does NOT inject `@Value` fields — Spring is not running.

```java
// ❌ jwtExpiration stays 0L (Java default for long)
@InjectMocks
private AuthService authService; // has @Value("${jwt.expiration}") private long jwtExpiration;

// ✅ Fix — set it manually in @BeforeEach
@BeforeEach
void setUp() {
    ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
}
```

Same pattern for `JwtService`:
```java
@BeforeEach
void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secret", "CdJeCU+qbKJVbIGBZrepVjuT9wmfbw9Mqmz2O4jo0+c=");
    ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
}
```

### Strict Stubbing

`@ExtendWith(MockitoExtension.class)` uses strict stubbing by default. If you stub something that is never called, the test **fails**.

```java
// ❌ This stub is never called → UnnecessaryStubbingException
when(jwtService.generateToken(any())).thenReturn("token");
// (if code throws before reaching generateToken)

// ✅ Only stub what your code path actually calls
```

This is a feature — it keeps tests precise and catches copy-paste errors.

---

## 7. Service Layer Testing

### Full Example — `AuthServiceTest`

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;
    @Captor private ArgumentCaptor<AppUser> userCaptor;

    @BeforeEach
    void setUp() {
        // @Value fields must be set manually
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should encode password before saving — never store plain text")
        void shouldEncodePasswordBeforeSaving() {
            RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "password123");
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.register(request);

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$hashed");
            assertThat(userCaptor.getValue().getPassword()).doesNotContain("password123");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username exists")
        void shouldThrowWhenUsernameIsDuplicate() {
            RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "pass123");
            when(userRepository.existsByUsername("muny")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");

            verify(userRepository, never()).save(any());
        }
    }
}
```

### Testing Pagination

```java
@Test
@DisplayName("should return paginated tasks")
void shouldReturnPagedTasks() {
    // PageImpl is the concrete class that implements Page<T>
    Page<Task> fakePage = new PageImpl<>(
        List.of(incompleteTask, completedTask),
        PageRequest.of(0, 5),
        2  // total elements
    );

    when(taskRepository.findAll(any(Pageable.class))).thenReturn(fakePage);

    Page<Task> result = taskService.getAllTasks(null, 0, 5, "id", "asc");

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getNumber()).isEqualTo(0);
}
```

### Testing Void Methods

```java
@Test
@DisplayName("should call repository.delete() with the correct task")
void shouldDeleteTask() {
    when(taskRepository.findById(1L)).thenReturn(Optional.of(incompleteTask));

    // deleteTask() returns void — nothing to assert on return value
    taskService.deleteTask(1L);

    // Verify behavior instead
    verify(taskRepository, times(1)).delete((Task) incompleteTask);
    //                                              ↑ Cast required in Spring Data 4.x
    //                                                due to ambiguous overloads
}
```

### Testing Private Methods Indirectly

```java
// validateSortBy() is private — cannot test it directly
// Test through the public method that calls it:

@Test
@DisplayName("should throw IllegalArgumentException for invalid sort field")
void shouldThrowForInvalidSortField() {
    assertThatThrownBy(() ->
        taskService.getAllTasks(null, 0, 5, "invalidField", "asc")
    )
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("Invalid sort field");
}
```

> **Rule:** Never test private methods directly. Test them through the public methods that call them.

---

## 8. Repository Layer Testing

### Why Repository Tests?

Mocks cannot test SQL. Only a real database can verify:
- A `LIKE` query is case-insensitive
- A `JOIN` fetches related data correctly
- A `WHERE` clause filters correctly
- A Specification combines filters with AND

### Setup

Because `@DataJpaTest` slice is not available as a standalone artifact in Spring Boot 4.0.x, we use `@SpringBootTest` + `@Transactional`:

```java
@SpringBootTest
@ActiveProfiles("test")    // uses application-test.properties → todo_test_db
@Transactional             // each test rolls back automatically
class TaskRepositoryTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private UserRepository userRepository;

    private AppUser userMuny;

    @BeforeEach
    void setUp() {
        // Save parent (user) before child (task) — FK constraint
        userMuny = userRepository.save(new AppUser("muny", "muny@email.com", "$2a$hashed"));
        taskRepository.save(new Task("Buy groceries", false, TaskPriority.HIGH,
                            LocalDate.of(2027, 1, 15), userMuny));
    }
}
```

### Test Database Setup

`src/test/resources/application-test.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/todo_test_db
spring.datasource.username=Muny
spring.datasource.password=Muny168168
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

Create the database once:
```sql
CREATE DATABASE todo_test_db;
```

Flyway automatically runs all migrations on first test run.

### How Transaction Rollback Works

```
Test 1 begins → transaction opens
  @BeforeEach saves users + tasks
  Test body runs real SQL queries
  Assertions pass or fail
Test 1 ends   → @Transactional ROLLS BACK → database empty again

Test 2 begins → new transaction
  @BeforeEach saves fresh data
  ...
```

Each test starts with a clean database. No cleanup code needed.

### Testing Specifications (Dynamic Filters)

```java
@Test
@DisplayName("should filter by title — partial, case-insensitive")
void shouldFilterByTitle() {
    var spec = TaskSpecifications.withFilters("buy", null, null, null, null, null);
    Page<Task> result = taskRepository.findAll(spec, PageRequest.of(0, 10));

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("Buy groceries");
}

@Test
@DisplayName("should combine dueAfter and dueBefore as a date range")
void shouldFilterByDateRange() {
    var spec = TaskSpecifications.withFilters(
        null, null, null,
        LocalDate.of(2027, 1, 1),   // dueAfter
        LocalDate.of(2027, 2, 28),  // dueBefore
        null
    );
    Page<Task> result = taskRepository.findAll(spec, PageRequest.of(0, 10));

    assertThat(result.getContent()).hasSize(2); // only tasks in range
}
```

---

## 9. Controller Layer Testing with MockMvc

### What is MockMvc?

MockMvc simulates HTTP requests without starting a real server:

```
Real world:    Browser → HTTP → Tomcat → Controller → Response
MockMvc test:  Test    → MockMvc → Controller → Response
```

### Setup — Spring Boot 4.x Way

Because `@WebMvcTest` slice is not available as a standalone artifact in Spring Boot 4.0.x, we use `@SpringBootTest` + manual `MockMvc`:

```java
@SpringBootTest
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private WebApplicationContext context;

    // Spring Security's main filter bean — must be added manually.
    // Without this, security is NOT applied and all requests return 200.
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    // Mock controller dependencies
    @MockitoBean private TaskService taskService;
    @MockitoBean private TaskMapper taskMapper;

    // Mock security dependencies (needed to wire JwtAuthFilter)
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private static final String VALID_TOKEN   = "valid-test-token";
    private static final String INVALID_TOKEN = "invalid-test-token";

    @BeforeEach
    void setUp() {
        // Build MockMvc with security filter applied
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .addFilter(springSecurityFilterChain)  // ← Critical for 401 tests
            .build();

        AppUser testUser = new AppUser("muny", "muny@email.com", "$2a$hashed");

        // Configure happy-path authentication
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("muny");
        when(customUserDetailsService.loadUserByUsername("muny")).thenReturn(testUser);
        when(jwtService.isTokenValid(VALID_TOKEN, testUser)).thenReturn(true);

        // Configure invalid token
        when(jwtService.extractUsername(INVALID_TOKEN)).thenReturn(null);
    }
}
```

> **Why `@MockitoBean` for `JwtService`?**
> `@SpringBootTest` loads `JwtAuthFilter` (it's a `@Component`). `JwtAuthFilter` depends on `JwtService` (it's a `@Service`). Since `@Service` beans are mocked by `@MockitoBean`, Spring can wire `JwtAuthFilter` correctly.

> **Why `FilterChainProxy`?**
> `MockMvcBuilders.webAppContextSetup(context)` creates a `DispatcherServlet` but does NOT automatically apply servlet filters (like Spring Security). The `FilterChainProxy` bean named `springSecurityFilterChain` must be added manually with `.addFilter()`.

### Writing MockMvc Tests

```java
@Nested
@DisplayName("POST /api/v1/tasks")
class CreateTask {

    @Test
    @DisplayName("should return 201 Created when request is valid")
    void shouldReturn201WhenCreated() throws Exception {
        when(taskService.createTask(any())).thenReturn(sampleTask);
        when(taskMapper.toResponse(sampleTask)).thenReturn(sampleResponse);

        mockMvc.perform(
            post("/api/v1/tasks")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Buy groceries",
                      "priority": "HIGH",
                      "dueDate": "2027-01-15",
                      "userId": 1
                    }
                    """)
        )
        .andExpect(status().isCreated())               // HTTP 201
        .andExpect(jsonPath("$.title").value("Buy groceries"))
        .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("should return 400 when title is blank")
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(
            post("/api/v1/tasks")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "", "priority": "HIGH", "userId": 1}""")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "test", "priority": "HIGH", "userId": 1}""")
        )
        .andExpect(status().isUnauthorized());  // HTTP 401
    }
}
```

### HTTP Status Assertion Methods

```java
status().isOk()            // 200
status().isCreated()       // 201
status().isNoContent()     // 204
status().isBadRequest()    // 400
status().isUnauthorized()  // 401
status().isForbidden()     // 403
status().isNotFound()      // 404
status().isConflict()      // 409
```

### JsonPath Assertions

```java
jsonPath("$.title").value("Buy groceries")      // field equals value
jsonPath("$.completed").value(false)             // boolean field
jsonPath("$.status").value(401)                  // number field
jsonPath("$.errors.username").exists()           // field exists
jsonPath("$.password").doesNotExist()            // field NOT in response (security!)
jsonPath("$.content").isArray()                  // is an array
jsonPath("$.content[0].title").value("...")      // first element of array
jsonPath("$.totalElements").value(1)             // pagination field
```

### MockMvc Request Methods

```java
// HTTP methods
get("/api/v1/tasks")
post("/api/v1/tasks")
put("/api/v1/tasks/1")
patch("/api/v1/tasks/1/complete")
delete("/api/v1/tasks/1")

// Adding to requests
.header("Authorization", "Bearer " + token)
.contentType(MediaType.APPLICATION_JSON)
.content("""{ "title": "..." }""")
.param("page", "0")
.param("size", "5")
```

### `@MockitoBean` — Spring Boot 4.x

```java
// ❌ Spring Boot 3.x (deprecated in 4.x)
@MockBean
private TaskService taskService;

// ✅ Spring Boot 4.x
@MockitoBean
private TaskService taskService;
```

---

## 10. Integration Testing

### What Makes It Different

No `@MockitoBean` — every layer is real:

```
Test → MockMvc → Controller (real)
                    ↓
                Service (real)
                    ↓
                Repository (real)
                    ↓
                PostgreSQL (real test DB)
                    ↓
               Response ← Test asserts
```

### Setup

```java
@SpringBootTest
@ActiveProfiles("test")   // todo_test_db
class TaskIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;

    // Create ObjectMapper directly — not autowired
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .addFilter(springSecurityFilterChain)
            .build();

        // Must use TransactionTemplate for cleanup.
        // @BeforeEach runs outside Spring-managed transactions.
        // deleteAll() needs a transaction — TransactionTemplate provides one.
        new TransactionTemplate(transactionManager).execute(status -> {
            taskRepository.deleteAll();
            userRepository.deleteByUsername("integration_user");
            return null;
        });
    }
}
```

### Why Not `@Transactional` for Cleanup?

HTTP requests go through the full request lifecycle and **commit** their transaction before returning. So:
- `@Transactional` on the test method: rolls back what YOU did inside the test method
- HTTP-committed data: **already committed** before your rollback happens
- Result: `@Transactional` rollback misses HTTP-committed data

Solution: `TransactionTemplate` for explicit cleanup before each test.

### Full Flow Test Pattern

```java
@Test
@DisplayName("Full flow: register → login → create task → complete task")
void shouldCompleteFullTaskLifecycle() throws Exception {

    // Step 1: Register
    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"muny","email":"muny@test.com","password":"pass123"}"""))
    .andExpect(status().isCreated());

    // Step 2: Login — get real JWT token
    MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"muny","password":"pass123"}"""))
    .andExpect(status().isOk())
    .andReturn();

    // Extract token from JSON response
    String body  = loginResult.getResponse().getContentAsString();
    String token = objectMapper.readTree(body).get("accessToken").asText();

    // Step 3: Use token to create a task
    MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"title":"Buy milk","priority":"HIGH","userId":1}"""))
    .andExpect(status().isCreated())
    .andReturn();

    long taskId = objectMapper.readTree(
        createResult.getResponse().getContentAsString()
    ).get("id").asLong();

    // Step 4: Assert directly in the database
    assertThat(taskRepository.findById(taskId)).isPresent();

    // Step 5: Complete the task
    mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/complete")
        .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.completed").value(true));

    // Step 6: Verify in database
    assertThat(taskRepository.findById(taskId))
        .isPresent()
        .get()
        .matches(task -> task.isCompleted());
}
```

### `MvcResult` — Extracting Response Data

```java
// Get the raw result (don't consume with .andReturn() AND .andExpect() together for same field)
MvcResult result = mockMvc.perform(...).andExpect(status().isOk()).andReturn();

String body = result.getResponse().getContentAsString();

// Parse JSON
JsonNode json  = objectMapper.readTree(body);
String  token  = json.get("accessToken").asText();
long    taskId = json.get("id").asLong();
```

---

## 11. Spring Boot 4.x Testing Notes

### What Changed from Spring Boot 3.x

| Feature | Spring Boot 3.x | Spring Boot 4.x |
|---|---|---|
| Test slice — Web | `@WebMvcTest` (in `spring-boot-starter-test`) | Use `@SpringBootTest` + manual MockMvc |
| Test slice — JPA | `@DataJpaTest` (in `spring-boot-starter-test`) | Use `@SpringBootTest` + `@Transactional` |
| Mock beans | `@MockBean` | `@MockitoBean` |
| All-in-one test dep | `spring-boot-starter-test` | `spring-boot-starter-webmvc-test` (basic) |

### Spring Boot 4.x Workarounds Used in This Project

**For repository tests:**
```java
// ❌ Not available as standalone artifact
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)

// ✅ Workaround
@SpringBootTest
@ActiveProfiles("test")
@Transactional
```

**For controller tests:**
```java
// ❌ Not available as standalone artifact
@WebMvcTest(TaskController.class)

// ✅ Workaround
@SpringBootTest
@ActiveProfiles("test")
// + manual MockMvc with FilterChainProxy
```

**Applying Spring Security to MockMvc:**
```java
// ❌ Requires spring-security-test (not in classpath)
.apply(SecurityMockMvcConfigurers.springSecurity())

// ✅ Inject and add FilterChainProxy directly
@Autowired FilterChainProxy springSecurityFilterChain;
mockMvc = MockMvcBuilders.webAppContextSetup(context)
    .addFilter(springSecurityFilterChain)
    .build();
```

### Spring Data 4.x — Ambiguous `delete()` Method

`JpaSpecificationExecutor` in Spring Data 4.x added `delete(DeleteSpecification<T>)`. This causes ambiguity when verifying `delete()` calls in Mockito.

```java
// ❌ Ambiguous — compiler can't pick between delete(T) and delete(DeleteSpecification<T>)
verify(taskRepository).delete(incompleteTask);
verify(taskRepository, never()).delete(any());

// ✅ Fix — cast to the concrete type
verify(taskRepository).delete((Task) incompleteTask);
verify(taskRepository, never()).delete(any(Task.class));
```

---

## 12. Real Bugs Found by Tests

Every error in Phase 5 taught a real lesson. These were real bugs that tests caught:

### Bug 1: `isTokenValid()` throws instead of returning false

**Found by:** `JwtServiceTest.shouldReturnFalseForExpiredToken`

**Problem:** When token is expired, `extractUsername()` throws `ExpiredJwtException` before `isTokenExpired()` can return `false`.

**Fix:**
```java
// ❌ Before
public boolean isTokenValid(String token, UserDetails userDetails) {
    String username = extractUsername(token); // throws ExpiredJwtException!
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
}

// ✅ After — a boolean method must never throw
public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    } catch (JwtException | IllegalArgumentException e) {
        return false; // expired, malformed, wrong signature → invalid
    }
}
```

**Lesson:** A method named `isValid` should NEVER throw. Catch all invalid cases and return `false`.

---

### Bug 2: `jwtExpiration` is 0 in unit tests

**Found by:** `AuthServiceTest.shouldReturnTokenOnValidLogin`

**Problem:** `@Value("${jwt.expiration}")` is not injected when Spring is not running. The field defaults to `0L`.

**Fix:**
```java
@BeforeEach
void injectValueFields() {
    ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
}
```

**Lesson:** Any `@Value` field in a class under test must be set with `ReflectionTestUtils` in unit tests.

---

### Bug 3: Security returns 200 instead of 401

**Found by:** `TaskControllerTest.shouldReturn401WhenNoToken`

**Problem:** `MockMvcBuilders.webAppContextSetup(context).build()` does NOT automatically apply the Spring Security `FilterChainProxy`.

**Fix:**
```java
@Autowired
private FilterChainProxy springSecurityFilterChain;

mockMvc = MockMvcBuilders
    .webAppContextSetup(context)
    .addFilter(springSecurityFilterChain)  // ← This line was missing
    .build();
```

**Lesson:** MockMvc bypasses the servlet container. Spring Security must be added explicitly via `FilterChainProxy`.

---

### Bug 4: `deleteAll()` fails with no transaction in `@BeforeEach`

**Found by:** `TaskIntegrationTest` setup

**Problem:** `@BeforeEach` runs outside any Spring-managed transaction. `deleteAll()` needs an EntityManager which needs an active transaction.

**Fix:**
```java
new TransactionTemplate(transactionManager).execute(status -> {
    taskRepository.deleteAll();
    userRepository.deleteByUsername(USERNAME);
    return null;
});
```

**Lesson:** For integration test cleanup, use `TransactionTemplate`. `@Transactional` rollback doesn't help because HTTP requests already committed their data.

---

### Bug 5: `LazyInitializationException` on `task.getUser()`

**Found by:** `TaskIntegrationTest.shouldCompleteFullTaskLifecycle`

**Problem:** `taskRepository.findById(id)` has no `@EntityGraph`. When `TaskMapper` accesses `task.getUser().getUsername()` after the transaction closes, Hibernate can't initialize the lazy proxy.

**Fix:**
```java
// In TaskRepository
@Override
@EntityGraph(attributePaths = "user")  // ← Added
Optional<Task> findById(Long id);
```

**Lesson:** With `open-in-view=false`, any lazy relationship accessed outside the transaction throws. Every repository method used by a mapper that reads lazy fields needs `@EntityGraph`.

This bug was **invisible to all 83 other tests** — only the integration test caught it.

---

## 13. Quick Reference Card

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=AuthServiceTest

# Specific test method
./mvnw test -Dtest="AuthServiceTest#shouldRegisterSuccessfully"

# Skip tests during build
./mvnw package -DskipTests
```

### Test Annotation Cheat Sheet

```java
// Unit test (no Spring)
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock           // fake dependency
    @InjectMocks    // real object under test
    @Captor         // capture arguments
    @BeforeEach     // runs before each test
}

// Repository test (real DB)
@SpringBootTest
@ActiveProfiles("test")
@Transactional      // auto-rollback
class MyRepositoryTest {
    @Autowired      // real bean
}

// Controller test (mocked services)
@SpringBootTest
@ActiveProfiles("test")
class MyControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @MockitoBean    // mock Spring bean (Spring Boot 4.x)
    // Build MockMvc manually in @BeforeEach
}

// Integration test (everything real)
@SpringBootTest
@ActiveProfiles("test")
class MyIntegrationTest {
    @Autowired PlatformTransactionManager transactionManager;
    // Manual cleanup with TransactionTemplate
}
```

### Mockito Cheat Sheet

```java
// Stub
when(mock.method()).thenReturn(value);
when(mock.method()).thenThrow(new Exception());
when(mock.method()).thenAnswer(inv -> inv.getArgument(0));
doThrow(ex).when(mock).voidMethod();  // for void methods

// Verify
verify(mock).method();
verify(mock, times(2)).method();
verify(mock, never()).method();
verify(mock, atLeastOnce()).method();

// Capture
verify(mock).method(captor.capture());
MyType value = captor.getValue();

// Matchers
any()  anyString()  anyLong()  eq(value)  isNull()
```

### AssertJ Cheat Sheet

```java
assertThat(x).isEqualTo(y)
assertThat(x).isNotNull()
assertThat(x).isTrue() / .isFalse()
assertThat(x).isPositive() / .isZero()
assertThat(str).isNotBlank()
assertThat(str).contains("text")
assertThat(str).doesNotContain("pass")
assertThat(list).hasSize(3)
assertThat(list).isEmpty()
assertThat(list).allMatch(e -> condition)
assertThat(optional).isPresent()
assertThatThrownBy(() -> ...).isInstanceOf(Ex.class).hasMessage("...")
```

---

## 14. Common Mistakes Master List

### Unit Tests

| Mistake | Fix |
|---|---|
| Forgetting `@ExtendWith(MockitoExtension.class)` | `@Mock` and `@InjectMocks` do nothing without it |
| Using `@MockBean` instead of `@MockitoBean` | Spring Boot 4.x uses `@MockitoBean` |
| `@Value` field stays at `0` / `null` | Use `ReflectionTestUtils.setField()` in `@BeforeEach` |
| Stubbing something never called | Mockito strict mode fails the test — only stub what's used |
| `any()` mixed with literal args | All args must use matchers when any one does |
| Only testing happy path | Always test exception cases and edge cases too |
| Vague test names like `test1()` | Use names that describe behavior: `shouldThrowWhenExpired()` |

### Repository Tests

| Mistake | Fix |
|---|---|
| Saving child before parent | Always save user before task (FK constraint) |
| Expecting data from previous test | `@Transactional` rolls back — each test starts clean |
| Using `@SpringBootTest` without `@ActiveProfiles("test")` | Will connect to real `todo_db`, not `todo_test_db` |

### Controller Tests

| Mistake | Fix |
|---|---|
| Not adding `FilterChainProxy` | Security not applied, all requests return 200 |
| Forgetting `@MockitoBean JwtService` | `JwtAuthFilter` can't be wired, context fails to load |
| Not configuring security mocks in `@BeforeEach` | All authenticated requests return 401 unexpectedly |
| Not asserting `$.password doesNotExist()` | Security test — always verify password never returns |

### Integration Tests

| Mistake | Fix |
|---|---|
| `@Transactional` on integration test | HTTP commits before rollback — use `TransactionTemplate` |
| Hardcoding IDs like `/tasks/1` | Extract real ID from create response |
| Not verifying DB state | Only asserting HTTP response misses persistence bugs |
| Not cleaning up between tests | Tests pollute each other — clean in `@BeforeEach` |

### General

| Mistake | Fix |
|---|---|
| Boolean method throws instead of returning false | Catch exceptions internally, return `false` |
| Lazy proxy accessed outside transaction | Add `@EntityGraph` to the repository method |
| One-liner text block `"""..."""` | Java requires newline after opening `"""` |
| Ambiguous `delete()` in Spring Data 4.x | Cast: `delete((Task) entity)` or `delete(any(Task.class))` |

---

*Phase 5 Complete — 84 tests, all green.*
*Next: Phase 6 — Advanced Backend (SOLID, Design Patterns, Clean Architecture)*
