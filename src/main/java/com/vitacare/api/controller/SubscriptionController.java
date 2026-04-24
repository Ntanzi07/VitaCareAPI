package com.vitacare.api.controller;

import com.vitacare.api.model.Client;
import com.vitacare.api.model.Plan;
import com.vitacare.api.model.Subscription;
import com.vitacare.api.repository.ClientRepository;
import com.vitacare.api.repository.PlanRepository;
import com.vitacare.api.repository.SubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "subscriptions-controller")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final ClientRepository clientRepository;

    public SubscriptionController(SubscriptionRepository subscriptionRepository,
                                   PlanRepository planRepository,
                                   ClientRepository clientRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.clientRepository = clientRepository;
    }

    @Operation(summary = "Create a subscription")
    @ApiResponse(responseCode = "201", description = "Subscription created")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/subscriptions")
    public ResponseEntity<SubscriptionCreateResponse> create(@RequestBody SubscriptionCreateRequest request) {
        Plan plan = planRepository.findById(request.planId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        Client client = clientRepository.findById(request.clientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Subscription subscription = new Subscription();
        subscription.setClient(client);
        subscription.setPlan(plan);
        subscription.setStartDate(request.startDate());
        subscription.setEndDate(request.endDate());
        subscription.setStatus("ACTIVE");

        Subscription saved = subscriptionRepository.save(subscription);

        return ResponseEntity.status(HttpStatus.CREATED).body(new SubscriptionCreateResponse(
            saved.getId(),
            saved.getClient().getId(),
            saved.getPlan().getId(),
            saved.getStatus(),
            saved.getStartDate(),
            saved.getEndDate(),
            OffsetDateTime.now()
        ));
    }

    @Operation(summary = "List subscriptions by client")
    @ApiResponse(responseCode = "200", description = "Subscription list")
    @ApiResponse(responseCode = "404", description = "Client not found")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/clients/{clientId}/subscriptions")
    public ResponseEntity<SubscriptionListResponse> listByClient(@PathVariable UUID clientId) {
        clientRepository.findById(clientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        List<SubscriptionSummary> data = subscriptionRepository.findByClient_Id(clientId)

            .stream()
            .map(s -> new SubscriptionSummary(
                s.getId(),
                new PlanInfo(
                    s.getPlan().getId(),
                    s.getPlan().getName(),
                    s.getPlan().getPrice()
                ),
                s.getStatus(),
                s.getStartDate(),
                s.getEndDate()
            )).toList();

        return ResponseEntity.ok(new SubscriptionListResponse(data, new Meta(data.size())));
    }

    @Operation(summary = "Get subscription by ID")
    @ApiResponse(responseCode = "200", description = "Subscription details")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/subscriptions/{id}")
    public ResponseEntity<SubscriptionDetailResponse> getById(@PathVariable UUID id) {
        Subscription s = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        return ResponseEntity.ok(new SubscriptionDetailResponse(
            s.getId(),
            new ClientInfo(
                s.getClient().getId(),
                s.getClient().getName()
            ),
            new PlanInfo(
                s.getPlan().getId(),
                s.getPlan().getName(),
                s.getPlan().getPrice()
            ),
            s.getStatus(),
            s.getStartDate(),
            s.getEndDate(),
            OffsetDateTime.now() // TODO: adicionar createdAt na entidade Subscription
        ));
    }

    @Operation(summary = "Update subscription status")
    @ApiResponse(responseCode = "200", description = "Subscription updated")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/api/subscriptions/{id}")
    public ResponseEntity<SubscriptionUpdateResponse> update(
            @PathVariable UUID id,
            @RequestBody SubscriptionUpdateRequest request) {

        Subscription s = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (request.status() != null) s.setStatus(request.status());

        subscriptionRepository.save(s);

        return ResponseEntity.ok(new SubscriptionUpdateResponse(
            s.getId(),
            s.getStatus(),
            OffsetDateTime.now()
        ));
    }

    public record SubscriptionCreateRequest(
        UUID clientId,
        UUID planId,
        LocalDate startDate,
        LocalDate endDate
    ) {}

    public record SubscriptionCreateResponse(
        UUID id,
        UUID clientId,
        UUID planId,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt
    ) {}

    public record SubscriptionListResponse(
        List<SubscriptionSummary> data,
        Meta meta
    ) {}

    public record SubscriptionSummary(
        UUID id,
        PlanInfo plan,
        String status,
        LocalDate startDate,
        LocalDate endDate
    ) {}

    public record SubscriptionDetailResponse(
        UUID id,
        ClientInfo client,
        PlanInfo plan,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt
    ) {}

    public record SubscriptionUpdateRequest(String status) {}

    public record SubscriptionUpdateResponse(
        UUID id,
        String status,
        OffsetDateTime updatedAt
    ) {}

    public record PlanInfo(UUID id, String name, BigDecimal price) {}
    public record ClientInfo(UUID id, String name) {}
    public record Meta(int total) {}
}