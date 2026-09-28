package com.apilens.api.dto;

import com.apilens.domain.model.ApiContract;

import java.time.Instant;
import java.util.UUID;

public record ContractSummary(
        UUID id,
        String specificationVersion,
        String title,
        int endpointCount,
        int schemaCount,
        Instant discoveredAt
) {
    public static ContractSummary from(ApiContract contract) {
        return new ContractSummary(
                contract.getId(),
                contract.getSpecificationVersion(),
                contract.getTitle(),
                contract.getEndpoints().size(),
                contract.getSchemas().size(),
                contract.getDiscoveredAt()
        );
    }
}
