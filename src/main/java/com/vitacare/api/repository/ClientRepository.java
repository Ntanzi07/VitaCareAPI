package com.vitacare.api.repository;

import com.vitacare.api.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    List<Client> findAllByOrderByCreatedAtDesc();

    Optional<Client> findByUser_Id(UUID userId);

    Optional<Client> findByIdAndUser_Id(UUID id, UUID userId);
}
