package com.vitacare.api.controller;

import com.vitacare.api.model.Nutritionist;
import com.vitacare.api.repository.NutritionistRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/nutritionists")
@Tag(name = "nutritionists-controller")
public class NutritionistController {

    private final NutritionistRepository nutritionistRepository;

    public NutritionistController(NutritionistRepository nutritionistRepository) {
        this.nutritionistRepository = nutritionistRepository;
    }

    @Operation(summary = "List all nutritionists (paginated)")
    @ApiResponse(responseCode = "200", description = "List of nutritionists")
    @GetMapping
    public ResponseEntity<Page<NutritionistListResponse>> list(Pageable pageable) {
        Page<Nutritionist> page = nutritionistRepository.findAll(pageable);
        Page<NutritionistListResponse> response = page.map(nutritionist -> new NutritionistListResponse(
            nutritionist.getId(),
            nutritionist.getName(),
            nutritionist.getCrn(),
            nutritionist.getDescription(),
            nutritionist.getConsultationPrice(),
            BigDecimal.ZERO  // TODO: Calculate rating from reviews
        ));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get nutritionist by ID")
    @ApiResponse(responseCode = "200", description = "Nutritionist details")
    @ApiResponse(responseCode = "404", description = "Nutritionist not found")
    @GetMapping("/{id}")
    public ResponseEntity<NutritionistDetailResponse> getById(@PathVariable UUID id) {
        Nutritionist nutritionist = nutritionistRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        NutritionistDetailResponse response = new NutritionistDetailResponse(
            nutritionist.getId(),
            nutritionist.getName(),
            nutritionist.getCrn(),
            nutritionist.getDescription(),
            nutritionist.getConsultationPrice(),
            BigDecimal.ZERO,  // TODO: Calculate rating from reviews
            nutritionist.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new nutritionist profile")
    @ApiResponse(responseCode = "201", description = "Nutritionist created")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<NutritionistCreateResponse> create(@RequestBody NutritionistCreateRequest request) {
        Nutritionist nutritionist = new Nutritionist();
        nutritionist.setName(request.name());
        nutritionist.setCrn(request.crn());
        nutritionist.setDescription(request.description());
        nutritionist.setConsultationPrice(request.consultationPrice());

        Nutritionist saved = nutritionistRepository.save(nutritionist);

        NutritionistCreateResponse response = new NutritionistCreateResponse(
            saved.getId(),
            saved.getName(),
            saved.getCrn(),
            saved.getDescription(),
            saved.getConsultationPrice(),
            saved.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update nutritionist profile")
    @ApiResponse(responseCode = "200", description = "Nutritionist updated")
    @ApiResponse(responseCode = "404", description = "Nutritionist not found")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<NutritionistUpdateResponse> update(
        @PathVariable UUID id,
        @RequestBody NutritionistUpdateRequest request
    ) {
        Nutritionist nutritionist = nutritionistRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        if (request.name() != null) {
            nutritionist.setName(request.name());
        }
        if (request.crn() != null) {
            nutritionist.setCrn(request.crn());
        }
        if (request.description() != null) {
            nutritionist.setDescription(request.description());
        }
        if (request.consultationPrice() != null) {
            nutritionist.setConsultationPrice(request.consultationPrice());
        }

        Nutritionist updated = nutritionistRepository.save(nutritionist);

        NutritionistUpdateResponse response = new NutritionistUpdateResponse(
            updated.getId(),
            updated.getName(),
            updated.getCrn(),
            updated.getDescription(),
            updated.getConsultationPrice(),
            OffsetDateTime.now()  // TODO: Add updatedAt field to Nutritionist entity
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a nutritionist")
    @ApiResponse(responseCode = "200", description = "Nutritionist deleted successfully")
    @ApiResponse(responseCode = "404", description = "Nutritionist not found")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResponse> delete(@PathVariable UUID id) {
        Nutritionist nutritionist = nutritionistRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        nutritionistRepository.delete(nutritionist);
        return ResponseEntity.ok(new DeleteResponse("Nutritionist deleted successfully"));
    }

    public record NutritionistListResponse(
        UUID id,
        String name,
        String crn,
        String description,
        BigDecimal consultationPrice,
        BigDecimal rating
    ) {}

    public record NutritionistDetailResponse(
        UUID id,
        String name,
        String crn,
        String description,
        BigDecimal consultationPrice,
        BigDecimal rating,
        OffsetDateTime createdAt
    ) {}

    public record NutritionistCreateRequest(
        String name,
        String crn,
        String description,
        BigDecimal consultationPrice
    ) {}

    public record NutritionistCreateResponse(
        UUID id,
        String name,
        String crn,
        String description,
        BigDecimal consultationPrice,
        OffsetDateTime createdAt
    ) {}

    public record NutritionistUpdateRequest(
        String name,
        String crn,
        String description,
        BigDecimal consultationPrice
    ) {}

    public record NutritionistUpdateResponse(
        UUID id,
        String name,
        String crn,
        String description,
        BigDecimal consultationPrice,
        OffsetDateTime updatedAt
    ) {}

    public record DeleteResponse(String message) {}
}
