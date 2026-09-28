package com.apilens.domain.repository;

import com.apilens.domain.model.DiscoveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscoveryAttemptRepository extends JpaRepository<DiscoveryAttempt, UUID> {

    List<DiscoveryAttempt> findByApiIdOrderByAttemptedAtDesc(UUID apiId);
}
