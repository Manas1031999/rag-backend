package com.manas.rag.document;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** One uploaded PDF. The text itself lives in the vector_store table as chunks. */
@Entity
@Table(name = "documents", indexes = @Index(name = "idx_documents_workspace", columnList = "workspaceId"))
public class PdfDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int pageCount;

    @Column(nullable = false)
    private int chunkCount;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    protected PdfDocument() {
    }

    public PdfDocument(UUID workspaceId, String filename, long sizeBytes) {
        this.workspaceId = workspaceId;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
    }

    public void markIngested(int pageCount, int chunkCount) {
        this.pageCount = pageCount;
        this.chunkCount = chunkCount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getFilename() {
        return filename;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
