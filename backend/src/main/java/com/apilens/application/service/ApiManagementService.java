package com.apilens.application.service;

import com.apilens.api.dto.CreateApiRequest;
import com.apilens.domain.exception.ResourceNotFoundException;
import com.apilens.domain.model.Api;
import com.apilens.domain.model.User;
import com.apilens.domain.repository.ApiRepository;
import com.apilens.domain.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for API management (Phase 2 scope): registering,
 * listing, fetching and removing APIs. Contains the one piece of business
 * logic this phase has — resolving the (currently fixed) owning user —
 * so that when real authentication lands, only this method changes, not
 * every controller.
 */
@Service
public class ApiManagementService {

    private final ApiRepository apiRepository;
    private final UserRepository userRepository;

    public ApiManagementService(ApiRepository apiRepository, UserRepository userRepository) {
        this.apiRepository = apiRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Api createApi(CreateApiRequest request) {
        User owner = currentUser();
        Api api = new Api(
                UUID.randomUUID(),
                owner,
                request.name().trim(),
                request.baseUrl().trim(),
                blankToNull(request.openApiUrl()),
                blankToNull(request.documentationUrl())
        );
        return apiRepository.save(api);
    }

    @Transactional(readOnly = true)
    public Page<Api> listApis(Pageable pageable) {
        return apiRepository.findByOwnerId(currentUser().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Api getApi(UUID id) {
        return apiRepository.findById(id)
                .filter(api -> api.getOwner().getId().equals(currentUser().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No API found with id " + id));
    }

    @Transactional
    public void deleteApi(UUID id) {
        Api api = getApi(id);
        apiRepository.delete(api);
    }

    /**
     * Resolves the "current user". Authentication is not implemented yet
     * (see docs/adr/ADR-003-ownership-without-auth.md), so every request
     * is attributed to the seeded system user. This is the single place
     * that will change once real auth exists.
     */
    private User currentUser() {
        return userRepository.findById(User.SYSTEM_USER_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "System user is missing; check that Flyway migration V2 ran."));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
