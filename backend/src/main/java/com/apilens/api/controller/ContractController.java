package com.apilens.api.controller;

import com.apilens.api.dto.ContractDetail;
import com.apilens.api.dto.ContractSummary;
import com.apilens.application.service.ContractQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Read API for parsed contracts. Split from ApiController since these
 * resources (product spec section 20) are addressed by their own id,
 * not nested under /api/apis/{id} except for the listing endpoint.
 */
@RestController
public class ContractController {

    private final ContractQueryService contractQueryService;

    public ContractController(ContractQueryService contractQueryService) {
        this.contractQueryService = contractQueryService;
    }

    @GetMapping("/api/apis/{apiId}/contracts")
    public List<ContractSummary> listForApi(@PathVariable UUID apiId) {
        return contractQueryService.getContractsForApi(apiId).stream()
                .map(ContractSummary::from)
                .toList();
    }

    @GetMapping("/api/contracts/{id}")
    public ContractDetail get(@PathVariable UUID id) {
        return ContractDetail.from(contractQueryService.getContract(id));
    }
}
