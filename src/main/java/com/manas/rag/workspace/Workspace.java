package com.manas.rag.workspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspaces", indexes = @Index(name = "idx_workspaces_owner", columnList = "ownerId"))
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Workspace() {
    }

    public Workspace(String name, UUID ownerId) {
        this.name = name;
        this.ownerId = ownerId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
