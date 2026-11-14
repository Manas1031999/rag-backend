package com.manas.rag.workspace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Step 2: create workspace. */
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    public record CreateWorkspaceRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record WorkspaceResponse(UUID id, String name, Instant createdAt) {
        static WorkspaceResponse from(Workspace w) {
            return new WorkspaceResponse(w.getId(), w.getName(), w.getCreatedAt());
        }
    }

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        return WorkspaceResponse.from(workspaceService.create(request.name(), userId));
    }

    @GetMapping
    public List<WorkspaceResponse> list(@AuthenticationPrincipal UUID userId) {
        return workspaceService.list(userId).stream().map(WorkspaceResponse::from).toList();
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID workspaceId) {
        return WorkspaceResponse.from(workspaceService.getOwned(workspaceId, userId));
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID workspaceId) {
        workspaceService.delete(workspaceId, userId);
    }
}
