package com.nexoia.workspace.service;

import com.nexoia.workspace.dto.CreateWorkspaceRequest;
import com.nexoia.workspace.dto.WorkspaceResponse;
import com.nexoia.workspace.exception.WorkspaceNotFoundException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaces;

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> list(UUID ownerId) {
        return workspaces.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public WorkspaceResponse create(UUID ownerId, CreateWorkspaceRequest request) {
        Workspace workspace = workspaces.save(Workspace.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name(request.name().trim())
                .build());

        return response(workspace);
    }

    /**
     * Resolves a workspace owned by the caller, for use as a Knowledge Vault scope target.
     */
    @Transactional(readOnly = true)
    public Workspace ownedWorkspace(UUID ownerId, UUID workspaceId) {
        return workspaces.findByIdAndOwnerId(workspaceId, ownerId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private WorkspaceResponse response(Workspace value) {
        return new WorkspaceResponse(
                value.getId(), value.getName(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
