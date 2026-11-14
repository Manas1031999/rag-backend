package com.manas.rag.workspace;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.manas.rag.common.ApiException;
import com.manas.rag.document.PdfDocumentRepository;
import com.manas.rag.document.VectorMetadata;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final PdfDocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public WorkspaceService(WorkspaceRepository workspaceRepository, PdfDocumentRepository documentRepository,
            VectorStore vectorStore) {
        this.workspaceRepository = workspaceRepository;
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    public Workspace create(String name, UUID ownerId) {
        return workspaceRepository.save(new Workspace(name.trim(), ownerId));
    }

    public List<Workspace> list(UUID ownerId) {
        return workspaceRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    /** Every workspace lookup goes through here, so one user can never reach another user's data. */
    public Workspace getOwned(UUID workspaceId, UUID ownerId) {
        return workspaceRepository.findByIdAndOwnerId(workspaceId, ownerId)
                .orElseThrow(() -> ApiException.notFound("Workspace not found."));
    }

    @Transactional
    public void delete(UUID workspaceId, UUID ownerId) {
        Workspace workspace = getOwned(workspaceId, ownerId);
        vectorStore.delete(VectorMetadata.workspaceFilter(workspace.getId()));
        documentRepository.deleteByWorkspaceId(workspace.getId());
        workspaceRepository.delete(workspace);
    }
}
