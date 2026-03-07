package com.vitacare.api.controller;

import com.vitacare.api.model.User;
import com.vitacare.api.repository.UserRepository;
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

    @GetMapping
    public List<User> list() {
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody CreateUserRequest req) {
        User u = new User();
        u.setUsername(req.getUsername());
        User saved = repo.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    public static class CreateUserRequest {
        @NotBlank
        private String username;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
