package com.manas.rag.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.manas.rag.user.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Reads "Authorization: Bearer <token>" and, if valid, marks the request as logged in. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            jwtService.parseUserId(header.substring(PREFIX.length()).trim())
                    .filter(userRepository::existsById)
                    .ifPresent(userId -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(userId, null, List.of())));
        }
        chain.doFilter(request, response);
    }
}
