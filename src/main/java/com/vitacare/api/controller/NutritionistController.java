package com.vitacare.api.controller;

import com.vitacare.api.model.User;
import com.vitacare.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class NutritionistController {

    private final NutricionistRepository repo;

    public NutritionistController(NutricionistRepository repo) {
        this.repo = repo;
    }

    @Operation(summary = "Get all nutritionists")
    @ApiResponse(responseCode = "200", description = "List of nutritionists")
    @GetMapping
    public Page<NutritionistListResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(user -> new NutritionistListResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt()
        ));
    }

    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "200", description = "User deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResponse> delete(@PathVariable UUID id) {
        User user = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        repo.delete(user);
        return ResponseEntity.ok(new DeleteResponse(true));
    }

    public record UserListResponse(UUID id, String email, String role, OffsetDateTime createdAt) {}

    public record DeleteResponse(boolean deleted) {}
}
