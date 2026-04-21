package com.vitacare.api.controller;

import com.vitacare.api.model.User;
import com.vitacare.api.repository.UserRepository;
import com.vitacare.api.security.AuthenticatedUser;
import com.vitacare.api.security.RoleFlags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "List of users")
    @GetMapping
    public Page<UserListResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(user -> new UserListResponse(
            user.getId(),
            user.getEmail(),
            user.getRoleMask(),
            RoleFlags.toNames(user.getRoleMask()),
            user.getCreatedAt()
        ));
    }

    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "200", description = "User deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResponse> delete(@PathVariable UUID id, Authentication authentication) {
        AuthenticatedUser principal = requireAuthenticatedUser(authentication);
        boolean isAdmin = RoleFlags.hasRole(principal.roleMask(), RoleFlags.ADMIN);
        boolean isOwner = principal.id().equals(id);
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        User user = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        repo.delete(user);
        return ResponseEntity.ok(new DeleteResponse(true));
    }

    private static AuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return principal;
    }

    public record UserListResponse(UUID id, String email, Integer roleMask, List<String> roles, OffsetDateTime createdAt) {}

    public record DeleteResponse(boolean deleted) {}
}
