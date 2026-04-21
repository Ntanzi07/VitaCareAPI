package com.vitacare.api.repository;

import com.vitacare.api.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByNutritionistId(UUID nutritionistId);

}