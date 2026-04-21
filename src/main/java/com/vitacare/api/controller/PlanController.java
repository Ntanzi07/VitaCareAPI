package com.vitacare.api.controller;

import com.vitacare.api.model.Nutritionist;
import com.vitacare.api.model.Plan;
import com.vitacare.api.repository.NutritionistRepository;
import com.vitacare.api.repository.PlanRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@Tag(name = "plans-controller")
public class PlanController {

    private final PlanRepository planRepository;
    private final NutritionistRepository nutritionistRepository;

    public PlanController(PlanRepository planRepository, NutritionistRepository nutritionistRepository) {
        this.planRepository = planRepository;
        this.nutritionistRepository = nutritionistRepository;
    }

    @Operation(summary = "Get plan by ID")
    @ApiResponse(responseCode = "200", description = "Plan details")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @GetMapping("/api/plans/{id}")
    public ResponseEntity<PlanDetailResponse> getById(@PathVariable UUID id) {
        Plan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        return ResponseEntity.ok(new PlanDetailResponse(
            plan.getId(),
            plan.getName(),
            plan.getDescription(),
            plan.getPrice(),
            plan.getDurationDays(),
            plan.getNutritionist().getId(),
            plan.getCreatedAt()
        ));
    }

    @Operation(summary = "List plans by nutritionist (paginated)")
    @ApiResponse(responseCode = "200", description = "List of plans")
    @ApiResponse(responseCode = "404", description = "Nutritionist not found")
    @GetMapping("/api/nutritionists/{nutritionistId}/plans")
    public ResponseEntity<Page<PlanSummaryResponse>> listByNutritionist(
            @PathVariable UUID nutritionistId,
            @PageableDefault(size = 10, sort = {}) Pageable pageable) {

        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        Page<PlanSummaryResponse> response = planRepository.findByNutritionist(nutritionist, pageable)
            .map(plan -> new PlanSummaryResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDurationDays(),
                plan.getCreatedAt()
            ));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a plan for a nutritionist")
    @ApiResponse(responseCode = "201", description = "Plan created")
    @ApiResponse(responseCode = "404", description = "Nutritionist not found")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/nutritionists/{nutritionistId}/plans")
    public ResponseEntity<PlanSummaryResponse> create(
            @PathVariable UUID nutritionistId,
            @RequestBody PlanCreateRequest request) {

        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        Plan plan = new Plan();
        plan.setNutritionist(nutritionist);
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setDurationDays(request.durationDays());

        Plan saved = planRepository.save(plan);

        return ResponseEntity.status(HttpStatus.CREATED).body(new PlanSummaryResponse(
            saved.getId(),
            saved.getName(),
            saved.getDescription(),
            saved.getPrice(),
            saved.getDurationDays(),
            saved.getCreatedAt()
        ));
    }

    @Operation(summary = "Update a plan")
    @ApiResponse(responseCode = "200", description = "Plan updated")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/api/plans/{id}")
    public ResponseEntity<PlanUpdateResponse> update(
            @PathVariable UUID id,
            @RequestBody PlanUpdateRequest request) {

        Plan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        if (request.name() != null) plan.setName(request.name());
        if (request.description() != null) plan.setDescription(request.description());
        if (request.price() != null) plan.setPrice(request.price());
        if (request.durationDays() != null) plan.setDurationDays(request.durationDays());

        Plan updated = planRepository.save(plan);

        return ResponseEntity.ok(new PlanUpdateResponse(
            updated.getId(),
            updated.getName(),
            updated.getDescription(),
            updated.getPrice(),
            updated.getDurationDays(),
            OffsetDateTime.now() // TODO: Add updatedAt field to Plan entity
        ));
    }

    @Operation(summary = "Delete a plan")
    @ApiResponse(responseCode = "200", description = "Plan deleted")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/plans/{id}")
    public ResponseEntity<DeleteResponse> delete(@PathVariable UUID id) {
        Plan plan = planRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        planRepository.delete(plan);
        return ResponseEntity.ok(new DeleteResponse(true));
    }

    public record PlanSummaryResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        OffsetDateTime createdAt
    ) {}

    public record PlanDetailResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        UUID nutritionistId,
        OffsetDateTime createdAt
    ) {}

    public record PlanCreateRequest(
        String name,
        String description,
        BigDecimal price,
        Integer durationDays
    ) {}

    public record PlanUpdateRequest(
        String name,
        String description,
        BigDecimal price,
        Integer durationDays
    ) {}

    public record PlanUpdateResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        OffsetDateTime updatedAt
    ) {}

    public record DeleteResponse(boolean deleted) {}
}