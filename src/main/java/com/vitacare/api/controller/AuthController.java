package com.vitacare.api.controller;

import com.vitacare.api.model.User;
import com.vitacare.api.repository.UserRepository;
import com.vitacare.api.security.AuthTokenService;
import com.vitacare.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Set<String> ALLOWED_ROLES = Set.of("CLIENT", "NUTRITIONIST", "ADMIN");

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, AuthTokenService authTokenService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authTokenService = authTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(normalizeRole(request.role(), "CLIENT"));

        User saved = userRepository.save(user);
        String token = authTokenService.issueToken(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(token, toAuthUserResponse(saved)));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = authTokenService.issueToken(user);
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", toAuthUserResponse(user)));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User user = userRepository.findById(principal.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new MeResponse(user.getId(), user.getEmail(), user.getRole(), null, null);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRole(String role, String fallback) {
        String resolved = role == null || role.isBlank() ? fallback : role.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(resolved)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
        return resolved;
    }

    private static AuthUserResponse toAuthUserResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    public record RegisterRequest(@NotBlank String email, @NotBlank String password, String role) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RegisterResponse(String token, AuthUserResponse user) {
    }

    public record LoginResponse(String token, String type, AuthUserResponse user) {
    }

    public record AuthUserResponse(UUID id, String email, String role) {
    }

    public record MeResponse(UUID id, String email, String role, ProfileResponse client, ProfileResponse nutritionist) {
    }

    public record ProfileResponse(UUID id, String name, String description, String type) {
    }
}