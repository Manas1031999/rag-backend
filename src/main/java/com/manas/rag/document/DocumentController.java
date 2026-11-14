package com.manas.rag.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.manas.rag.common.ApiException;
import com.manas.rag.workspace.WorkspaceService;

/** Step 3: upload PDF (the service then runs steps 4 to 7). */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/documents")
public class DocumentController {

    public record DocumentResponse(UUID id, String filename, long sizeBytes, int pageCount, int chunkCount,
            Instant uploadedAt) {
        static DocumentResponse from(PdfDocument d) {
            return new DocumentResponse(d.getId(), d.getFilename(), d.getSizeBytes(), d.getPageCount(),
                    d.getChunkCount(), d.getUploadedAt());
        }
    }

    private final WorkspaceService workspaceService;
    private final IngestionService ingestionService;
    private final PdfDocumentRepository documentRepository;

    public DocumentController(WorkspaceService workspaceService, IngestionService ingestionService,
            PdfDocumentRepository documentRepository) {
        this.workspaceService = workspaceService;
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@AuthenticationPrincipal UUID userId, @PathVariable UUID workspaceId,
            @RequestPart("file") MultipartFile file) {
        workspaceService.getOwned(workspaceId, userId);
        return DocumentResponse.from(ingestionService.ingest(workspaceId, file));
    }

    @GetMapping
    public List<DocumentResponse> list(@AuthenticationPrincipal UUID userId, @PathVariable UUID workspaceId) {
        workspaceService.getOwned(workspaceId, userId);
        return documentRepository.findByWorkspaceIdOrderByUploadedAtDesc(workspaceId).stream()
                .map(DocumentResponse::from).toList();
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID workspaceId,
            @PathVariable UUID documentId) {
        workspaceService.getOwned(workspaceId, userId);
        PdfDocument document = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Document not found."));
        ingestionService.delete(document);
    }
}
