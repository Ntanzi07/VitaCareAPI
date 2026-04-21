package com.vitacare.api.repository;

import com.vitacare.api.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {

}