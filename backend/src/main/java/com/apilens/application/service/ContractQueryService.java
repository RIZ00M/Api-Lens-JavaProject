package com.apilens.application.service;

import com.apilens.domain.exception.ResourceNotFoundException;
import com.apilens.domain.model.Api;
import com.apilens.domain.model.ApiContract;
import com.apilens.domain.repository.ApiContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read access to parsed contracts. Ownership is enforced transitively:
 * a contract/endpoint can only be reached by first proving the caller
 * owns the API it belongs to (ApiManagementService.getApi already does
 * this), same pattern as DiscoveryApplicationService.
 */
@Service
public class ContractQueryService {

    private final ApiManagementService apiManagementService;
    private final ApiContractRepository apiContractRepository;

    public ContractQueryService(ApiManagementService apiManagementService,
                                 ApiContractRepository apiContractRepository) {
        this.apiManagementService = apiManagementService;
        this.apiContractRepository = apiContractRepository;
    }

    @Transactional(readOnly = true)
    public List<ApiContract> getContractsForApi(UUID apiId) {
        apiManagementService.getApi(apiId); // ownership check
        return apiContractRepository.findByApiIdOrderByDiscoveredAtDesc(apiId);
    }

    @Transactional(readOnly = true)
    public ApiContract getContract(UUID contractId) {
        ApiContract contract = apiContractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("No contract found with id " + contractId));
        assertOwnership(contract.getApi());
        return contract;
    }

    private void assertOwnership(Api api) {
        apiManagementService.getApi(api.getId()); // throws if not owned/missing
    }
}
