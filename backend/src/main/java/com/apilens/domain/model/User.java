package com.apilens.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Minimal user record. Real authentication (JWT/OAuth2) is not
 * implemented yet — see docs/adr/ADR-003-ownership-without-auth.md — but
 * every API is owned by a User from day one so that ownership does not
 * need to be retrofitted later.
 */
@Entity
@Table(name = "users")
public class User {

    /** Fixed id of the system user that owns every API until real auth exists. */
    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
