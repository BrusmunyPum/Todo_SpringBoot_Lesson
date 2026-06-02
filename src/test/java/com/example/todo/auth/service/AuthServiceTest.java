package com.example.todo.auth.service;

import com.example.todo.auth.dto.request.LoginRequest;
import com.example.todo.auth.dto.request.RegisterRequest;
import com.example.todo.auth.dto.response.LoginResponse;
import com.example.todo.common.exception.DuplicateResourceException;
import com.example.todo.common.exception.InvalidCredentialsException;
import com.example.todo.common.security.JwtService;
import com.example.todo.user.entity.AppUser;
import com.example.todo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Step 1: Tell JUnit 5 to activate Mockito for this test class
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Step 2: Declare mocks — Mockito creates these automatically
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    // Step 3: Declare the real object — Mockito injects the @Mock fields into it
    @InjectMocks
    private AuthService authService;

    // Step 4: ArgumentCaptor — captures what was passed to a mock method
    // We use this to inspect the AppUser object that was saved to the repository
    @Captor
    private ArgumentCaptor<AppUser> userCaptor;

    // Step 5: Inject @Value fields that Spring would normally fill
    // @InjectMocks creates a real AuthService, but Mockito does NOT process
    // @Value annotations — that's Spring's job. So we set the field manually.
    @BeforeEach
    void injectValueFields() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
    }

    // ─────────────────────────────────────────────────────────────────────
    // REGISTER TESTS
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register successfully when username and email are unique")
        void shouldRegisterSuccessfully() {
            // ── Arrange ───────────────────────────────────────────────────
            RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "password123");

            when(userRepository.existsByUsername("muny")).thenReturn(false);
            when(userRepository.existsByEmail("muny@email.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
            when(userRepository.save(any(AppUser.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            // thenAnswer: instead of returning a fixed value,
            // we run a lambda. invocation.getArgument(0) means:
            // "return back the first argument that was passed to save()"
            // This simulates what a real repository does: saves and returns the entity.

            // ── Act ───────────────────────────────────────────────────────
            AppUser result = authService.register(request);

            // ── Assert ────────────────────────────────────────────────────
            assertThat(result.getUsername()).isEqualTo("muny");
            assertThat(result.getEmail()).isEqualTo("muny@email.com");
            assertThat(result.getPassword()).isEqualTo("$2a$hashed");
        }

        @Test
        @DisplayName("should save the user with an encoded password, not plain text")
        void shouldSaveUserWithEncodedPassword() {
            // ── Arrange ───────────────────────────────────────────────────
            RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "password123");

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
            when(userRepository.save(any(AppUser.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ── Act ───────────────────────────────────────────────────────
            authService.register(request);

            // ── Assert with ArgumentCaptor ────────────────────────────────
            // Capture the AppUser object that was passed to save()
            verify(userRepository).save(userCaptor.capture());
            AppUser capturedUser = userCaptor.getValue();

            // Now we can inspect exactly what was saved
            assertThat(capturedUser.getUsername()).isEqualTo("muny");
            assertThat(capturedUser.getEmail()).isEqualTo("muny@email.com");
            assertThat(capturedUser.getPassword()).isEqualTo("$2a$hashed");
            // Most important: raw password must NOT be saved
            assertThat(capturedUser.getPassword()).doesNotContain("password123");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username already exists")
        void shouldThrowWhenUsernameIsDuplicate() {
            // ── Arrange ───────────────────────────────────────────────────
            RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "password123");
            when(userRepository.existsByUsername("muny")).thenReturn(true);

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Username already exists");

            // save() must never be called — we stopped before reaching it
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void shouldThrowWhenEmailIsDuplicate() {
            // ── Arrange ───────────────────────────────────────────────────
            RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "password123");
            when(userRepository.existsByUsername("muny")).thenReturn(false);
            when(userRepository.existsByEmail("muny@email.com")).thenReturn(true);

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should check username before email — username check comes first")
        void shouldCheckUsernameBeforeEmail() {
            // ── Arrange ───────────────────────────────────────────────────
            RegisterRequest request = new RegisterRequest("muny", "muny@email.com", "password123");

            // Both username AND email are duplicate — but username is checked first
            when(userRepository.existsByUsername("muny")).thenReturn(true);

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> authService.register(request))
                    .hasMessageContaining("Username already exists"); // not "Email"

            // Email check should never be reached
            verify(userRepository, never()).existsByEmail(anyString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGIN TESTS
    // ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should return a token and expiry when credentials are correct")
        void shouldReturnTokenOnValidLogin() {
            // ── Arrange ───────────────────────────────────────────────────
            LoginRequest request = new LoginRequest("muny", "password123");

            AppUser user = new AppUser("muny", "muny@email.com", "$2a$hashed");
            when(userRepository.findByUsername("muny")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("fake.jwt.token");

            // ── Act ───────────────────────────────────────────────────────
            LoginResponse response = authService.login(request);

            // ── Assert ────────────────────────────────────────────────────
            assertThat(response.getAccessToken()).isEqualTo("fake.jwt.token");
            assertThat(response.getExpiresIn()).isPositive();
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            // ── Arrange ───────────────────────────────────────────────────
            LoginRequest request = new LoginRequest("nobody", "password123");
            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid username or password");

            // If user not found, we must NOT check the password
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException when password is wrong")
        void shouldThrowWhenPasswordIsWrong() {
            // ── Arrange ───────────────────────────────────────────────────
            LoginRequest request = new LoginRequest("muny", "wrongpassword");

            AppUser user = new AppUser("muny", "muny@email.com", "$2a$hashed");
            when(userRepository.findByUsername("muny")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", "$2a$hashed")).thenReturn(false);

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid username or password");

            // Token must NEVER be generated for a failed login
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("should call generateToken with the correct user object")
        void shouldCallGenerateTokenWithCorrectUser() {
            // ── Arrange ───────────────────────────────────────────────────
            LoginRequest request = new LoginRequest("muny", "password123");

            AppUser user = new AppUser("muny", "muny@email.com", "$2a$hashed");
            when(userRepository.findByUsername("muny")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("token");

            // ── Act ───────────────────────────────────────────────────────
            authService.login(request);

            // ── Assert ────────────────────────────────────────────────────
            // Verify generateToken was called with exactly the right user object
            // eq(user) means: the EXACT same user object, not just any AppUser
            verify(jwtService, times(1)).generateToken(eq(user));
        }

        @Test
        @DisplayName("should give the same error message for wrong username vs wrong password")
        void shouldGiveSameMessageForBothWrongCases() {
            // Security: never tell the caller WHICH field was wrong
            // ── Arrange ───────────────────────────────────────────────────
            LoginRequest wrongUser  = new LoginRequest("nobody", "pass");
            LoginRequest wrongPass  = new LoginRequest("muny",   "wrong");

            when(userRepository.findByUsername("nobody"))
                    .thenReturn(Optional.empty());

            AppUser user = new AppUser("muny", "muny@email.com", "$2a$hashed");
            when(userRepository.findByUsername("muny"))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "$2a$hashed"))
                    .thenReturn(false);

            // ── Act & Assert ──────────────────────────────────────────────
            assertThatThrownBy(() -> authService.login(wrongUser))
                    .hasMessage("Invalid username or password");

            assertThatThrownBy(() -> authService.login(wrongPass))
                    .hasMessage("Invalid username or password");
        }
    }
}
