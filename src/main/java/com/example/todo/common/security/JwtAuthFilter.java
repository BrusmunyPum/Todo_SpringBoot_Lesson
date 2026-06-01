package com.example.todo.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Read the Authorization header
        String authHeader = request.getHeader("Authorization");

        // 2. If there is no Bearer token, skip — let Spring Security handle authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // 4. If token is blank/empty, skip — do NOT throw
        if (token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 5. Extract the username from the token
            String username = jwtService.extractUsername(token);

            // 6. If we got a username and user is not yet authenticated in this request
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 7. Load the user from the database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 8. Validate the token (signature + expiry)
                if (jwtService.isTokenValid(token, userDetails)) {

                    // 9. Create an authentication object and set it in the SecurityContext
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            // Token is invalid, expired, or malformed.
            // We do NOT throw — we simply clear the context and continue.
            // Spring Security will handle unauthorized access for protected endpoints.
            SecurityContextHolder.clearContext();
        }

        // 10. Continue to the next filter regardless
        filterChain.doFilter(request, response);
    }
}
