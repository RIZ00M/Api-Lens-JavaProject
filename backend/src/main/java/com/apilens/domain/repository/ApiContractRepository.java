package com.apilens.domain.repository;

import com.apilens.domain.model.ApiContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApiContractRepository extends JpaRepository<ApiContract, UUID> {

    List<ApiContract> findByApiIdOrderByDiscoveredAtDesc(UUID apiId);
}
