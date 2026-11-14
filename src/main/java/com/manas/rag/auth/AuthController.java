package com.manas.rag.auth;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.manas.rag.auth.AuthDtos.AuthResponse;
import com.manas.rag.auth.AuthDtos.LoginRequest;
import com.manas.rag.auth.AuthDtos.RegisterRequest;
import com.manas.rag.auth.AuthDtos.UserResponse;
import com.manas.rag.common.ApiException;
import com.manas.rag.security.JwtService;
import com.manas.rag.user.User;
import com.manas.rag.user.UserRepository;

import jakarta.validation.Valid;

/** Step 1: login / register. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        User user = userRepository.save(
                new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        return toResponse(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password."));
        return toResponse(user);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UUID userId) {
        return userRepository.findById(userId)
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail()))
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                jwtService.createToken(user.getId()),
                jwtService.expirySeconds(),
                new UserResponse(user.getId(), user.getName(), user.getEmail()));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
