package com.vitacare.api.controller;

import com.vitacare.api.model.Client;
import com.vitacare.api.model.User;
import com.vitacare.api.repository.ClientRepository;
import com.vitacare.api.repository.UserRepository;
import com.vitacare.api.security.AuthenticatedUser;
import com.vitacare.api.security.RoleFlags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public ClientController(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "List clients")
    @ApiResponse(responseCode = "200", description = "List of clients")
    @GetMapping
    public ClientsListResponse list(Authentication authentication) {
        AuthenticatedUser currentUser = requireAuthenticatedUser(authentication);
        if (!isAdmin(currentUser) && !isNutritionist(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN and NUTRITIONIST users can access this route");
        }
        List<ClientSummaryResponse> data = clientRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(ClientController::toSummaryResponse)
            .toList();
        return new ClientsListResponse(data, new MetaResponse(data.size()));
    }

    @Operation(summary = "Get a client by id")
    @ApiResponse(responseCode = "200", description = "Client details")
    @GetMapping("/{id}")
    public ClientDetailResponse getById(@PathVariable UUID id, Authentication authentication) {
        AuthenticatedUser currentUser = requireAuthenticatedUser(authentication);
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        // ADMIN can access all clients
        // NUTRITIONIST can access all clients
        // Other users can only access their own client
        boolean isOwner = client.getUser().getId().equals(currentUser.id());
        if (!isAdmin(currentUser) && !isNutritionist(currentUser) && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        return toDetailResponse(client);
    }

    @Operation(summary = "Update client profile")
    @ApiResponse(responseCode = "200", description = "Client updated")
    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody UpsertClientRequest request, Authentication authentication) {
        AuthenticatedUser currentUser = requireAuthenticatedUser(authentication);
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        // Only the owner of the client can update it
        if (!client.getUser().getId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        applyRequest(client, request);
        client.setUpdatedAt(OffsetDateTime.now());

        return toResponse(clientRepository.save(client));
    }

    private static ClientSummaryResponse toSummaryResponse(Client client) {
        return new ClientSummaryResponse(
            client.getId(),
            client.getName(),
            client.getBirthDate(),
            client.getHeight(),
            client.getWeight(),
            client.getCreatedAt()
        );
    }

    private static ClientResponse toResponse(Client client) {
        return new ClientResponse(
            client.getId(),
            client.getName(),
            client.getBirthDate(),
            client.getHeight(),
            client.getWeight(),
            client.getCreatedAt(),
            client.getUpdatedAt()
        );
    }

    private static ClientDetailResponse toDetailResponse(Client client) {
        return new ClientDetailResponse(
            client.getId(),
            client.getName(),
            client.getBirthDate(),
            client.getHeight(),
            client.getWeight(),
            client.getCreatedAt(),
            client.getUpdatedAt(),
            new UserRefResponse(client.getUser().getId(), client.getUser().getEmail())
        );
    }

    private static void applyRequest(Client client, UpsertClientRequest request) {
        client.setName(request.name());
        client.setBirthDate(request.birthDate());
        client.setHeight(request.height());
        client.setWeight(request.weight());
    }

    private static AuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return principal;
    }

    private static AuthenticatedUser requireClient(Authentication authentication) {
        AuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (!RoleFlags.hasRole(principal.roleMask(), RoleFlags.CLIENT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CLIENT users can access this route");
        }
        return principal;
    }

    private static void requireNutritionist(Authentication authentication) {
        AuthenticatedUser principal = requireAuthenticatedUser(authentication);
        if (!RoleFlags.hasRole(principal.roleMask(), RoleFlags.NUTRITIONIST)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only NUTRITIONIST users can access this route");
        }
    }

    private static boolean isNutritionist(AuthenticatedUser principal) {
        return RoleFlags.hasRole(principal.roleMask(), RoleFlags.NUTRITIONIST);
    }

    private static boolean isAdmin(AuthenticatedUser principal) {
        return RoleFlags.hasRole(principal.roleMask(), RoleFlags.ADMIN);
    }

    public record UpsertClientRequest(@NotBlank String name, @NotNull LocalDate birthDate, @NotNull BigDecimal height, @NotNull BigDecimal weight) {}

    public record ClientResponse(UUID id, String name, LocalDate birthDate, BigDecimal height, BigDecimal weight, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record ClientDetailResponse(UUID id, String name, LocalDate birthDate, BigDecimal height, BigDecimal weight, OffsetDateTime createdAt, OffsetDateTime updatedAt, UserRefResponse user) {}

    public record ClientSummaryResponse(UUID id, String name, LocalDate birthDate, BigDecimal height, BigDecimal weight, OffsetDateTime createdAt) {}

    public record ClientsListResponse(List<ClientSummaryResponse> data, MetaResponse meta) {}

    public record MetaResponse(long total) {}

    public record UserRefResponse(UUID id, String email) {}
}
