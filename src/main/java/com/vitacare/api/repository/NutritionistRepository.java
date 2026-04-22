package com.vitacare.api.repository;

import com.vitacare.api.model.Nutritionist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NutritionistRepository extends JpaRepository<Nutritionist, UUID> {

    Optional<Nutritionist> findByUserId(UUID userId);

    List<Nutritionist> findByNameContainingIgnoreCase(String name);

    boolean existsByCrn(String crn);
}