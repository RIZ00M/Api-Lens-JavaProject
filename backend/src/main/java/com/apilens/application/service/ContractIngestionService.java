package com.apilens.application.service;

import com.apilens.discovery.model.DiscoveryResult;
import com.apilens.domain.model.Api;
import com.apilens.domain.model.ApiContract;
import com.apilens.domain.repository.ApiContractRepository;
import com.apilens.infrastructure.openapi.ContractParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Bridges discovery and parsing: given a DiscoveryResult that found a
 * specification, parses its body via ContractParser and persists the
 * resulting ApiContract. Kept separate from DiscoveryApplicationService
 * so "found a spec" and "successfully understood it" stay distinguishable
 * -- a spec can be found (Phase 4's job) but fail to parse (a malformed
 * or unsupported document), and callers need to tell those apart.
 */
@Service
public class ContractIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ContractIngestionService.class);

    private final ContractParser contractParser;
    private final ApiContractRepository apiContractRepository;

    public ContractIngestionService(ContractParser contractParser, ApiContractRepository apiContractRepository) {
        this.contractParser = contractParser;
        this.apiContractRepository = apiContractRepository;
    }

    /** @return empty if the result didn't find a spec at all; otherwise the parse/persist outcome. */
    public Optional<IngestionOutcome> ingest(Api api, DiscoveryResult discoveryResult) {
        if (!discoveryResult.specificationFound() || discoveryResult.specificationBody() == null) {
            return Optional.empty();
        }

        ContractParser.ParseResult parseResult = contractParser.parse(
                api, discoveryResult.sourceUrl(), discoveryResult.specificationBody());

        if (!parseResult.success()) {
            log.warn("Parsing failed for API {} at {}: {}", api.getId(), discoveryResult.sourceUrl(),
                    parseResult.errors());
            return Optional.of(new IngestionOutcome(null, parseResult.errors()));
        }

        ApiContract saved = apiContractRepository.save(parseResult.contract());
        return Optional.of(new IngestionOutcome(saved, parseResult.errors()));
    }

    public record IngestionOutcome(ApiContract contract, List<String> messages) {
        public boolean isSuccess() {
            return contract != null;
        }
    }
}
