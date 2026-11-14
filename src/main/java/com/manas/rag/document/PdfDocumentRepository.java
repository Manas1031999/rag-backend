package com.manas.rag.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PdfDocumentRepository extends JpaRepository<PdfDocument, UUID> {

    List<PdfDocument> findByWorkspaceIdOrderByUploadedAtDesc(UUID workspaceId);

    Optional<PdfDocument> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    void deleteByWorkspaceId(UUID workspaceId);
}
