# Phase 4 — Authentication & Security Review
> Spring Boot 4.x · Spring Security 7.x · JWT (JJWT 0.12.6)
> Project: Task Management API

---

## Table of Contents

1. [Spring Security Basics](#1-spring-security-basics)
2. [Password Hashing](#2-password-hashing)
3. [Register API](#3-register-api)
4. [Login API](#4-login-api)
5. [JWT Access Token](#5-jwt-access-token)
6. [Security Filter Chain](#6-security-filter-chain)
7. [Current User Endpoint](#7-current-user-endpoint)
8. [Role-Based Authorization](#8-role-based-authorization)
9. [CORS Configuration](#9-cors-configuration)
10. [Logout Strategy](#10-logout-strategy)
11. [Common Security Mistakes](#11-common-security-mistakes)
12. [Full Flow Diagram](#12-full-flow-diagram)
13. [Quick Reference](#13-quick-reference)

---

## 1. Spring Security Basics

### What it does
Spring Security intercepts every HTTP request through a **filter chain** before it reaches your controller.

### Key concepts

| Concept | Meaning |
|---|---|
| Filter Chain | Series of filters that process every request |
| Authentication | Verifying WHO you are (login) |
| Authorization | Verifying WHAT you are allowed to do |
| Stateless | Server stores no session — JWT carries identity |

### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-gson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

> ⚠️ **Spring Boot 4 note:** Use `jjwt-gson`, NOT `jjwt-jackson`.
> `jjwt-jackson` depends on Jackson 2, which conflicts with Spring Boot 4's Jackson 3.

---

## 2. Password Hashing

### Why we hash passwords
Never store raw passwords. If the database is leaked, hashed passwords cannot be reversed.

### BCrypt
```java
// In SecurityConfig
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

```java
// Hash when saving
passwordEncoder.encode("secret123")
// → "$2a$10$N9qo8uLOickgx2ZMRZo..."

// Verify when logging in
passwordEncoder.matches("secret123", hashedPassword)
// → true or false
```

### Key rule
> BCrypt produces a **different hash every time** for the same password.
> This is intentional — it uses a random salt.
> `matches()` handles this automatically.

---

## 3. Register API

### Endpoint
```
POST /api/v1/auth/register
```

### Request body
```json
{
  "username": "muny",
  "email": "muny@example.com",
  "password": "secret123"
}
```

### Response `201 Created`
```json
{
  "id": 1,
  "username": "muny",
  "email": "muny@example.com"
}
```

### AuthService.register()
```java
@Transactional
public AppUser register(RegisterRequest request) {
    // 1. Check duplicate username
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new DuplicateResourceException("Username already exists");
    }
    // 2. Check duplicate email
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateResourceException("Email already exists");
    }
    // 3. Hash password + save
    AppUser newUser = new AppUser(
        request.getUsername(),
        request.getEmail(),
        passwordEncoder.encode(request.getPassword())  // ← hash
    );
    return userRepository.save(newUser);
}
```

### Validation rules (RegisterRequest)
```java
@NotBlank @Size(min = 3, max = 50)  String username;
@NotBlank @Email                    String email;
@NotBlank @Size(min = 6)            String password;
```

---

## 4. Login API

### Endpoint
```
POST /api/v1/auth/login
```

### Request body
```json
{
  "username": "muny",
  "password": "secret123"
}
```

### Response `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### AuthService.login()
```java
public LoginResponse login(LoginRequest request) {
    // 1. Find user — throws InvalidCredentialsException if not found
    AppUser user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

    // 2. Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new InvalidCredentialsException("Invalid username or password");
    }

    // 3. Generate JWT
    String token = jwtService.generateToken(user);
    return new LoginResponse(token, jwtExpiration / 1000);
}
```

> ⚠️ **Important:** We do NOT use `authenticationManager.authenticate()` here.
> `BadCredentialsException` extends `AuthenticationException`.
> Spring Security's `ExceptionTranslationFilter` intercepts `AuthenticationException`
> BEFORE our `GlobalExceptionHandler` can handle it.
> Manual credential check avoids this problem entirely.

### Wrong error messages (Security rule)
```java
// ❌ NEVER — reveals which usernames exist
throw new Exception("User not found");
throw new Exception("Wrong password");

// ✅ ALWAYS — same message for both cases
throw new InvalidCredentialsException("Invalid username or password");
```

---

## 5. JWT Access Token

### What is JWT
JSON Web Token — a self-contained string that proves who you are.

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtdW55IiwiaWF0IjoxNjk...}.<signature>
      Header                    Payload                      Signature
```

Decoded payload:
```json
{
  "sub": "muny",
  "iat": 1748700000,
  "exp": 1748786400
}
```

### application.properties
```properties
jwt.secret=CdJeCU+qbKJVbIGBZrepVjuT9wmfbw9Mqmz2O4jo0+c=
jwt.expiration=86400000
```

> `jwt.secret` must be Base64-encoded and at least 256 bits (32 bytes) for HS256.

### JwtService
```java
// Generate token
public String generateToken(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
}

// Validate token
public boolean isTokenValid(String token, UserDetails userDetails) {
    String username = extractUsername(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
}

// Signing key
private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
}
```

### How to use the token in requests
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 6. Security Filter Chain

### Full configuration
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### Spring Security 7 breaking change
```java
// ❌ Old (Spring Security 6) — compile error in Spring Security 7
DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
provider.setUserDetailsService(userDetailsService);

// ✅ New (Spring Security 7) — pass UserDetailsService in constructor
DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
provider.setPasswordEncoder(passwordEncoder());
```

### AppUser implements UserDetails
```java
@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }
    // Spring Security 6+ has default true implementations for the
    // boolean methods (isAccountNonExpired, isEnabled, etc.)
}
```

### JwtAuthFilter
```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Skip if no Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Skip if token is blank
        if (token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Bad token → clear context and continue
            // Spring Security will handle unauthorized access for protected endpoints
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
```

> ⚠️ **Rule:** JwtAuthFilter must NEVER throw. Bad token = clear context + continue.

### CustomAuthenticationEntryPoint
Called when request has no valid authentication for a protected endpoint:
```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            "{\"status\":401,\"message\":\"Authentication required. Please provide a valid Bearer token.\"}"
        );
    }
}
```

### Error codes
| Situation | Code | Who handles it |
|---|---|---|
| No token / invalid token on protected endpoint | 401 | `CustomAuthenticationEntryPoint` |
| Valid token but wrong role | 403 | `GlobalExceptionHandler` |
| Token expired (parsed in JwtAuthFilter) | 401 | `CustomAuthenticationEntryPoint` |

---

## 7. Current User Endpoint

### How to get the current user
```java
// ✅ Best way — Spring injects the authenticated user directly
@GetMapping("/me")
public ResponseEntity<UserResponse> getMe(
        @AuthenticationPrincipal AppUser currentUser
) {
    return ResponseEntity.ok(userMapper.toResponse(currentUser));
}
```

> `@AuthenticationPrincipal` works because `AppUser` implements `UserDetails`
> and is stored in `SecurityContextHolder` by `JwtAuthFilter`.

### Manual way (avoid in controllers)
```java
// Only use this in services when you can't use @AuthenticationPrincipal
Object principal = SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getPrincipal();
AppUser currentUser = (AppUser) principal;
```

### Me endpoints
```
GET /api/v1/users/me                  → my profile
GET /api/v1/users/me/detail           → my profile + task count
GET /api/v1/users/me/tasks            → my tasks (paginated)
GET /api/v1/users/me/tasks/completed  → my completed tasks
```

> **Rule:** Always use `/me` endpoints for user's own data.
> Never trust `userId` sent by the client — always read from the JWT.

---

## 8. Role-Based Authorization

### UserRole enum
```java
public enum UserRole {
    USER,   // normal user
    ADMIN   // full access
}
```

### Role field in AppUser
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private UserRole role = UserRole.USER; // default for all new users
```

### getAuthorities() — the ROLE_ prefix
```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    // USER  → "ROLE_USER"
    // ADMIN → "ROLE_ADMIN"
}
```

> `hasRole('ADMIN')` checks for `"ROLE_ADMIN"` in authorities automatically.
> `hasAuthority('ROLE_ADMIN')` requires the full string.

### Enable method security
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ← required for @PreAuthorize to work
public class SecurityConfig { ... }
```

### Protecting endpoints
```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")   // ← runs before method body
public ResponseEntity<List<UserResponse>> getAllUsers() { ... }
```

### Flyway migration V6
```sql
ALTER TABLE app_users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

### How to promote a user to ADMIN
```sql
UPDATE app_users SET role = 'ADMIN' WHERE username = 'muny';
```
Then **login again** to get a new token with `ROLE_ADMIN`.

### AccessDeniedException handler
```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex) {
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(new ApiError(403, "You do not have permission to perform this action"));
}
```

---

## 9. CORS Configuration

### What is CORS
Browser security rule. Different port = different origin = browser blocks the response.

```
Next.js  localhost:3000  →  Spring Boot  localhost:3000 ≠ localhost:8080 = BLOCKED
```

> Postman and curl are NOT browsers — they ignore CORS.

### application.properties
```properties
cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

### CorsConfigurationSource bean
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOrigins(allowedOrigins);       // specific origins only
    config.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));
    config.setAllowedHeaders(List.of(
        "Authorization", "Content-Type", "Accept"
    ));
    config.setExposedHeaders(List.of("Authorization"));
    config.setAllowCredentials(true);               // required for JWT headers
    config.setMaxAge(3600L);                        // cache preflight 1 hour

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

### Wire into SecurityFilterChain
```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

### Key rules
```java
// ❌ FORBIDDEN combination
config.setAllowedOrigins(List.of("*"));
config.setAllowCredentials(true);
// → Browser rejects this

// ✅ Must use specific origins when allowCredentials = true
config.setAllowedOrigins(List.of("http://localhost:3000"));
config.setAllowCredentials(true);
```

### Test CORS with curl
```bash
# Test preflight
curl -v -X OPTIONS http://localhost:8080/api/v1/tasks \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization"

# Expected response headers:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
# Access-Control-Allow-Credentials: true
```

---

## 10. Logout Strategy

### Why JWT logout is different
JWT is stateless — server holds no session. Calling logout does not invalidate the token server-side.

### The 3 strategies

| Strategy | How | Complexity |
|---|---|---|
| **Client-side** | Frontend deletes token | Simple ✅ |
| **Blacklist (Redis)** | Server stores invalidated token IDs | Medium |
| **Short expiry + refresh** | 15 min access + 7 day refresh | Complex |

### Current implementation (client-side)
```java
@PostMapping("/logout")
public ResponseEntity<Void> logout() {
    // Server does nothing — client must delete the token
    return ResponseEntity.noContent().build(); // 204
}
```

### What the frontend must do
```javascript
async function logout() {
    await fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    });

    // THIS is the real logout
    localStorage.removeItem('token');  // or clear React state
    router.push('/login');
}
```

---

## 11. Common Security Mistakes

| # | Mistake | Fix |
|---|---|---|
| 1 | Secrets in code / Git | Use environment variables |
| 2 | Different error for wrong username vs wrong password | Always same message: "Invalid username or password" |
| 3 | Storing plain-text passwords | Always `passwordEncoder.encode()` |
| 4 | Weak JWT secret (short string) | Minimum 256-bit (32 bytes) Base64 key |
| 5 | Trusting `userId` from request body | Read from JWT via `@AuthenticationPrincipal` |
| 6 | No JWT filter | `JwtAuthFilter` must run on every request |
| 7 | HTTP sessions with JWT | Use `SessionCreationPolicy.STATELESS` |
| 8 | CSRF enabled on JWT REST API | Disable CSRF — sessions are required for CSRF attacks |
| 9 | `allowedOrigins("*")` with `allowCredentials(true)` | Forbidden — use specific origins |
| 10 | No global exception handler | Always return clean JSON errors |

---

## 12. Full Flow Diagram

### Register
```
POST /api/v1/auth/register
  → Validate request body
  → Check username/email not duplicate
  → Hash password with BCrypt
  → Save to DB
  → Return 201 { id, username, email }
```

### Login
```
POST /api/v1/auth/login
  → Validate request body
  → Find user by username (404 → 401 "Invalid credentials")
  → BCrypt.matches(rawPassword, hashedPassword) (no → 401)
  → Generate JWT token
  → Return 200 { accessToken, tokenType, expiresIn }
```

### Protected request
```
GET /api/v1/tasks
  Authorization: Bearer eyJ...

  → JwtAuthFilter
      → Extract token from header
      → Extract username from token
      → Load user from DB
      → Validate token (signature + expiry)
      → Set SecurityContext
  → AuthorizationFilter
      → Check anyRequest().authenticated() → OK
  → TaskController.getAllTasks()
  → Return 200 { content: [...] }
```

### Protected request — no token
```
GET /api/v1/tasks  (no Authorization header)

  → JwtAuthFilter → skip (no header)
  → AuthorizationFilter → unauthenticated → 401
  → CustomAuthenticationEntryPoint
  → Return 401 { "Authentication required..." }
```

### Protected request — wrong role
```
GET /api/v1/users  (ROLE_USER token)

  → JwtAuthFilter → sets ROLE_USER in SecurityContext
  → AuthorizationFilter → passes (authenticated)
  → @PreAuthorize("hasRole('ADMIN')") → FAIL
  → AccessDeniedException
  → GlobalExceptionHandler
  → Return 403 { "You do not have permission..." }
```

---

## 13. Quick Reference

### Endpoints summary

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/api/v1/auth/register` | POST | ❌ | Register new user |
| `/api/v1/auth/login` | POST | ❌ | Login, get JWT |
| `/api/v1/auth/logout` | POST | ✅ | Logout (client-side) |
| `/api/v1/users/me` | GET | ✅ | My profile |
| `/api/v1/users/me/detail` | GET | ✅ | My profile + stats |
| `/api/v1/users/me/tasks` | GET | ✅ | My tasks |
| `/api/v1/users` | GET | 🔒 ADMIN | All users |
| `/api/v1/users/{id}` | GET | 🔒 ADMIN | User by ID |

### HTTP status codes used in Phase 4

| Code | Meaning | When |
|---|---|---|
| 200 | OK | Successful login |
| 201 | Created | Successful register |
| 204 | No Content | Logout |
| 401 | Unauthorized | No token / invalid token / wrong credentials |
| 403 | Forbidden | Valid token but wrong role |
| 409 | Conflict | Duplicate username or email |

### Files created in Phase 4

```
src/main/java/com/example/todo/
├── auth/
│   ├── controller/AuthController.java          ← register, login, logout
│   ├── service/AuthService.java                ← register + manual login
│   ├── service/CustomUserDetailsService.java   ← loads user from DB for Spring Security
│   └── dto/
│       ├── request/LoginRequest.java
│       ├── request/RegisterRequest.java
│       ├── response/LoginResponse.java
│       └── response/RegisterResponse.java
├── common/
│   ├── security/
│   │   ├── SecurityConfig.java                 ← filter chain + CORS + DaoAuthProvider
│   │   ├── JwtService.java                     ← generate + validate tokens
│   │   ├── JwtAuthFilter.java                  ← intercepts every request
│   │   └── CustomAuthenticationEntryPoint.java ← 401 JSON response
│   └── exception/
│       └── InvalidCredentialsException.java    ← plain RuntimeException for login failure
├── user/
│   └── entity/
│       ├── AppUser.java                        ← implements UserDetails, has role field
│       └── UserRole.java                       ← USER, ADMIN enum
└── resources/
    ├── application.properties                  ← jwt.secret, jwt.expiration, cors.allowed-origins
    └── db/migration/
        └── V6__add_role_to_users.sql           ← adds role column DEFAULT 'USER'
```

### Key annotations

| Annotation | Where | What it does |
|---|---|---|
| `@EnableWebSecurity` | SecurityConfig | Enables Spring Security |
| `@EnableMethodSecurity` | SecurityConfig | Enables `@PreAuthorize` |
| `@PreAuthorize("hasRole('ADMIN')")` | Controller method | Blocks non-admin users |
| `@AuthenticationPrincipal` | Controller parameter | Injects current logged-in user |
| `@Enumerated(EnumType.STRING)` | AppUser.role | Stores "USER"/"ADMIN" as text in DB |

---

*Phase 4 Complete ✅ — Next: Phase 5 Testing*
