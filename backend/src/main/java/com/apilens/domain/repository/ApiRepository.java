package com.apilens.domain.repository;

import com.apilens.domain.model.Api;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApiRepository extends JpaRepository<Api, UUID> {

    Page<Api> findByOwnerId(UUID ownerId, Pageable pageable);
}
