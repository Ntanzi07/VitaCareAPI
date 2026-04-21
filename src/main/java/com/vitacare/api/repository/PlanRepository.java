package com.vitacare.api.repository;

import com.vitacare.api.model.Nutritionist;
import com.vitacare.api.model.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Page<Plan> findByNutritionist(Nutritionist nutritionist, Pageable pageable);
}