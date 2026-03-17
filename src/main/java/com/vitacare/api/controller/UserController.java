  package com.vitacare.api.controller;

import com.vitacare.api.model.User;
import com.vitacare.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "List of users")
    @GetMapping
    public List<User> list() {
        return repo.findAll();
    }

    @Operation(summary = "Create a new user")
    @ApiResponse(responseCode = "201", description = "User created")
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody CreateUserRequest req) {
        User u = new User();
        u.setEmail(req.getEmail());
        // minimal defaults to satisfy DB NOT NULL constraints
        u.setPasswordHash(req.getPassword() != null ? req.getPassword() : "");
        u.setRole(req.getRole() != null ? req.getRole() : "CLIENT");
        User saved = repo.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    public static class CreateUserRequest {
        @NotBlank
        private String email;

        private String password;

        private String role;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
