package com.manas.rag.chat;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.manas.rag.workspace.WorkspaceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Step 8: the user asks a question. */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/chat")
public class ChatController {

    public record ChatRequest(@NotBlank @Size(max = 2000) String question) {
    }

    private final WorkspaceService workspaceService;
    private final RagService ragService;

    public ChatController(WorkspaceService workspaceService, RagService ragService) {
        this.workspaceService = workspaceService;
        this.ragService = ragService;
    }

    @PostMapping
    public RagService.Answer ask(@AuthenticationPrincipal UUID userId, @PathVariable UUID workspaceId,
            @Valid @RequestBody ChatRequest request) {
        workspaceService.getOwned(workspaceId, userId);
        return ragService.ask(workspaceId, request.question().trim());
    }
}
