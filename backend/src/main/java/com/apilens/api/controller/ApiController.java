package com.apilens.api.controller;

import com.apilens.api.dto.ApiSummary;
import com.apilens.api.dto.CreateApiRequest;
import com.apilens.api.dto.DiscoveryAttemptSummary;
import com.apilens.api.dto.DiscoveryResultResponse;
import com.apilens.application.service.ApiManagementService;
import com.apilens.application.service.DiscoveryApplicationService;
import com.apilens.domain.model.Api;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST API for API management (create / list / fetch / delete) and, from
 * Phase 4, discovery. Thin by design: request validation happens via Bean
 * Validation on the DTO, everything else happens in the application
 * services this controller delegates to.
 */
@RestController
@RequestMapping("/api/apis")
public class ApiController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ApiManagementService apiManagementService;
    private final DiscoveryApplicationService discoveryApplicationService;

    public ApiController(ApiManagementService apiManagementService,
                          DiscoveryApplicationService discoveryApplicationService) {
        this.apiManagementService = apiManagementService;
        this.discoveryApplicationService = discoveryApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiSummary> create(@Valid @RequestBody CreateApiRequest request) {
        Api created = apiManagementService.createApi(request);
        ApiSummary body = ApiSummary.from(created);
        return ResponseEntity.created(URI.create("/api/apis/" + body.id())).body(body);
    }

    @GetMapping
    public Page<ApiSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return apiManagementService.listApis(pageable).map(ApiSummary::from);
    }

    @GetMapping("/{id}")
    public ApiSummary get(@PathVariable UUID id) {
        return ApiSummary.from(apiManagementService.getApi(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        apiManagementService.deleteApi(id);
    }

    @PostMapping("/{id}/discover")
    public DiscoveryResultResponse discover(@PathVariable UUID id) {
        return DiscoveryResultResponse.from(discoveryApplicationService.runDiscovery(id));
    }

    @GetMapping("/{id}/discovery-attempts")
    public List<DiscoveryAttemptSummary> discoveryAttempts(@PathVariable UUID id) {
        return discoveryApplicationService.getAttempts(id).stream()
                .map(DiscoveryAttemptSummary::from)
                .toList();
    }
}
