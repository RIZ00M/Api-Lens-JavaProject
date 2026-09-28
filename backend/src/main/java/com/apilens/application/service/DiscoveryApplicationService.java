package com.apilens.application.service;

import com.apilens.discovery.DiscoveryEngine;
import com.apilens.discovery.model.DiscoveryAttemptOutcome;
import com.apilens.discovery.model.DiscoveryContext;
import com.apilens.discovery.model.DiscoveryResult;
import com.apilens.domain.model.Api;
import com.apilens.domain.model.DiscoveryAttempt;
import com.apilens.domain.repository.DiscoveryAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs discovery for a given API, persists every attempt made (product
 * spec section 5), and -- if a specification was found -- hands it to
 * ContractIngestionService to parse and persist. Kept synchronous for
 * now -- see docs/architecture.md, "Phase 4 scope", for why the async
 * job/202-Accepted pattern (section 30) is deferred rather than built here.
 */
@Service
public class DiscoveryApplicationService {

    private final ApiManagementService apiManagementService;
    private final DiscoveryEngine discoveryEngine;
    private final DiscoveryAttemptRepository discoveryAttemptRepository;
    private final ContractIngestionService contractIngestionService;

    public DiscoveryApplicationService(ApiManagementService apiManagementService,
                                        DiscoveryEngine discoveryEngine,
                                        DiscoveryAttemptRepository discoveryAttemptRepository,
                                        ContractIngestionService contractIngestionService) {
        this.apiManagementService = apiManagementService;
        this.discoveryEngine = discoveryEngine;
        this.discoveryAttemptRepository = discoveryAttemptRepository;
        this.contractIngestionService = contractIngestionService;
    }

    @Transactional
    public DiscoveryOutcome runDiscovery(UUID apiId) {
        Api api = apiManagementService.getApi(apiId); // throws ResourceNotFoundException if missing/not owned

        DiscoveryContext context = new DiscoveryContext(
                api.getId(), api.getBaseUrl(), api.getOpenApiUrl(), api.getDocumentationUrl());

        DiscoveryResult result = discoveryEngine.discover(context);

        for (DiscoveryAttemptOutcome outcome : result.attempts()) {
            discoveryAttemptRepository.save(new DiscoveryAttempt(
                    UUID.randomUUID(),
                    api,
                    outcome.url(),
                    outcome.discoveryMethod(),
                    outcome.httpStatus(),
                    outcome.contentType(),
                    outcome.responseSizeBytes(),
                    outcome.success(),
                    outcome.errorMessage(),
                    outcome.confidence(),
                    outcome.detectedFormat()
            ));
        }

        Optional<ContractIngestionService.IngestionOutcome> ingestion = contractIngestionService.ingest(api, result);
        return new DiscoveryOutcome(result, ingestion);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryAttempt> getAttempts(UUID apiId) {
        apiManagementService.getApi(apiId); // ownership check
        return discoveryAttemptRepository.findByApiIdOrderByAttemptedAtDesc(apiId);
    }

    /** Discovery's own result plus (if a spec was found) what parsing it produced. */
    public record DiscoveryOutcome(
            DiscoveryResult discoveryResult,
            Optional<ContractIngestionService.IngestionOutcome> ingestionOutcome
    ) {
    }
}
