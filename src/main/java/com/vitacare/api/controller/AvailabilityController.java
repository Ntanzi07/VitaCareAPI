package com.vitacare.api.controller;

import com.vitacare.api.model.Nutritionist;
import com.vitacare.api.model.Availability;
import com.vitacare.api.repository.AvailabilityRepository;
import com.vitacare.api.repository.NutritionistRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nutritionists/{nutritionistId}/availability")
@Tag(name = "availability-controller")
public class AvailabilityController {

    private final AvailabilityRepository availabilityRepository;
    private final NutritionistRepository nutritionistRepository;

    public AvailabilityController(AvailabilityRepository availabilityRepository,
                                   NutritionistRepository nutritionistRepository) {
        this.availabilityRepository = availabilityRepository;
        this.nutritionistRepository = nutritionistRepository;
    }

    @Operation(summary = "List availability for a nutritionist")
    @ApiResponse(responseCode = "200", description = "Availability list")
    @ApiResponse(responseCode = "404", description = "Nutritionist not found")
    @GetMapping
    public ResponseEntity<AvailabilityListResponse> list(@PathVariable UUID nutritionistId) {
        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        List<AvailabilityResponse> list = nutritionist.getAvailability().stream()
            .map(a -> new AvailabilityResponse(
                a.getId(),
                a.getDayOfWeek(),
                a.getStartTime(),
                a.getEndTime(),
                true 
            )).toList();

        return ResponseEntity.ok(new AvailabilityListResponse(list));
    }

    @Operation(summary = "Add availability slot")
    @ApiResponse(responseCode = "201", description = "Availability created")
    @ApiResponse(responseCode = "404", description = "Nutritionist not found")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<AvailabilityCreateResponse> create(
            @PathVariable UUID nutritionistId,
            @RequestBody AvailabilityRequest request) {

        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        Availability availability = new Availability();
        availability.setNutritionist(nutritionist);
        availability.setDayOfWeek(request.dayOfWeek());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());

        Availability saved = availabilityRepository.save(availability);

        return ResponseEntity.status(HttpStatus.CREATED).body(new AvailabilityCreateResponse(
            saved.getId(),
            saved.getDayOfWeek(),
            saved.getStartTime(),
            saved.getEndTime(),
            OffsetDateTime.now()
        ));
    }

    @Operation(summary = "Update availability slot")
    @ApiResponse(responseCode = "200", description = "Availability updated")
    @ApiResponse(responseCode = "404", description = "Availability not found")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{availabilityId}")
    public ResponseEntity<AvailabilityUpdateResponse> update(
            @PathVariable UUID nutritionistId,
            @PathVariable UUID availabilityId,
            @RequestBody AvailabilityRequest request) {

        Availability availability = availabilityRepository.findById(availabilityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found"));

        if (request.dayOfWeek() != null) availability.setDayOfWeek(request.dayOfWeek());
        if (request.startTime() != null) availability.setStartTime(request.startTime());
        if (request.endTime() != null) availability.setEndTime(request.endTime());

        Availability updated = availabilityRepository.save(availability);

        return ResponseEntity.ok(new AvailabilityUpdateResponse(
            updated.getId(),
            updated.getDayOfWeek(),
            updated.getStartTime(),
            updated.getEndTime(),
            OffsetDateTime.now()
        ));
    }

    @Operation(summary = "Delete availability slot")
    @ApiResponse(responseCode = "200", description = "Availability deleted")
    @ApiResponse(responseCode = "404", description = "Availability not found")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<NutritionistAfterDeleteResponse> delete(
            @PathVariable UUID nutritionistId,
            @PathVariable UUID availabilityId) {

        Nutritionist nutritionist = nutritionistRepository.findById(nutritionistId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nutritionist not found"));

        Availability availability = availabilityRepository.findById(availabilityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found"));

        availabilityRepository.delete(availability);

        return ResponseEntity.ok(new NutritionistAfterDeleteResponse(
            nutritionist.getId(),
            nutritionist.getName(),
            nutritionist.getCrn(),
            nutritionist.getDescription(),
            nutritionist.getConsultationPrice(),
            OffsetDateTime.now()
        ));
    }

    public record AvailabilityListResponse(List<AvailabilityResponse> availability) {}

    public record AvailabilityResponse(
        UUID id,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean available
    ) {}

    public record AvailabilityCreateResponse(
        UUID id,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        OffsetDateTime createdAt
    ) {}

    public record AvailabilityUpdateResponse(
        UUID id,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        OffsetDateTime updatedAt
    ) {}

    public record NutritionistAfterDeleteResponse(
        UUID id,
        String name,
        String crn,
        String bio,
        BigDecimal consultationPrice,
        OffsetDateTime updatedAt
    ) {}

    public record AvailabilityRequest(
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {}
}