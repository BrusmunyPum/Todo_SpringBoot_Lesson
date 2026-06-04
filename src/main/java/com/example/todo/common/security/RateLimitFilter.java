package com.example.todo.common.security;

import com.example.todo.common.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits login and register attempts per IP address.
 *
 * Strategy: token-bucket algorithm (Bucket4j)
 *   - Each IP gets its own bucket
 *   - Bucket holds 10 tokens
 *   - Refills 10 tokens every 1 hour
 *   → Max 10 attempts/hour per IP before 429 is returned
 *
 * Only applied to POST /api/v1/auth/login and /api/v1/auth/register.
 * All other paths pass through untouched.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // One bucket per IP — ConcurrentHashMap is thread-safe for concurrent requests
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Build our own ObjectMapper with JavaTimeModule so it can serialize
    // LocalDateTime inside ApiError. We don't inject a bean here because
    // this project uses Gson (not Jackson) for its main serialization,
    // so Spring Boot doesn't register an ObjectMapper bean by default.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ─── Bucket config ────────────────────────────────────────────────────────

    private Bucket newBucket() {
        // 10 attempts per hour per IP
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // ─── Filter logic ─────────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        if (!isRateLimitedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            rejectWithTooManyRequests(response);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isRateLimitedPath(String path) {
        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register");
    }

    /**
     * Extracts the real client IP, respecting X-Forwarded-For
     * (set by proxies and load balancers like Nginx or Railway).
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Header may contain a chain: "clientIp, proxy1, proxy2"
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a 429 JSON response directly — we can't throw here because
     * we're in a filter, not a controller, so @ExceptionHandler won't fire.
     */
    private void rejectWithTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = new ApiError(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too many requests — you have exceeded 10 login attempts per hour. Please try again later."
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
