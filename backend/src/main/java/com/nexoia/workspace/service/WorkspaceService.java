package com.nexoia.workspace.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.workspace.config.WorkspaceStorage;
import com.nexoia.workspace.dto.BindWorkspaceRequest;
import com.nexoia.workspace.dto.CreateWorkspaceRequest;
import com.nexoia.workspace.dto.WorkspaceFileResponse;
import com.nexoia.workspace.dto.WorkspaceResponse;
import com.nexoia.workspace.dto.WorkspaceStatusResponse;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.exception.WorkspaceInvalidPathException;
import com.nexoia.workspace.exception.WorkspaceInUseException;
import com.nexoia.workspace.exception.WorkspaceNotFoundException;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStorageType;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner-scoped orchestration for Workspaces: create/list/get, bind to server storage, unbind/delete,
 * and read-only inspection (status, refresh, tree, file). Controllers stay thin; every path
 * resolution and containment check is delegated to {@link WorkspacePathResolver} and every listing to
 * {@link WorkspaceInspectionService}. Absolute paths never leave this layer.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaces;
    private final WorkspaceAccessService access;
    private final WorkspaceInspectionService inspection;
    private final WorkspacePathResolver pathResolver;
    private final WorkspaceStorage storage;
    private final AuditService audit;

    public List<WorkspaceResponse> list(UUID ownerId) {
        return workspaces.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public WorkspaceResponse create(UUID ownerId, CreateWorkspaceRequest request) {
        Workspace workspace = workspaces.saveAndFlush(Workspace.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name(request.name().trim())
                .build());

        return response(workspace);
    }

    public WorkspaceResponse get(UUID ownerId, UUID workspaceId) {
        return response(access.accessibleWorkspace(ownerId, workspaceId));
    }

    /**
     * Binds a Workspace to server storage. MANAGED creates the owner/workspace directory the server
     * owns; MOUNTED points at an existing project under the configured import root and, in this first
     * increment, is restricted to the Nexo Owner. The client never provides an absolute path.
     */
    public WorkspaceResponse bind(NexoUserPrincipal principal, UUID workspaceId, BindWorkspaceRequest request) {
        Workspace workspace = access.accessibleWorkspace(principal.userId(), workspaceId);
        switch (request.storageType()) {
            case MANAGED -> bindManaged(workspace, request.accessMode());
            case MOUNTED -> bindMounted(principal, workspace, request);
            case UNBOUND -> throw new WorkspaceInvalidPathException();
        }
        Workspace saved = workspaces.saveAndFlush(workspace);
        audit.record(RecordAuditCommand.success(
                AuditAction.WORKSPACE_BOUND, principal.userId(), null, AuditTargetType.WORKSPACE, workspaceId));
        return response(saved);
    }

    @Transactional
    public void delete(UUID ownerId, UUID workspaceId) {
        Workspace workspace = access.accessibleWorkspace(ownerId, workspaceId);
        try {
            workspaces.delete(workspace);
            workspaces.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new WorkspaceInUseException(exception);
        }
        audit.record(RecordAuditCommand.success(
                AuditAction.WORKSPACE_DELETED, ownerId, null, AuditTargetType.WORKSPACE, workspaceId));
    }

    public WorkspaceStatusResponse status(UUID ownerId, UUID workspaceId) {
        return inspection.status(access.accessibleWorkspace(ownerId, workspaceId));
    }

    public WorkspaceStatusResponse refresh(UUID ownerId, UUID workspaceId) {
        Workspace workspace = access.accessibleWorkspace(ownerId, workspaceId);
        WorkspaceStatusResponse result = inspection.refresh(workspace);
        workspaces.saveAndFlush(workspace);
        return result;
    }

    public WorkspaceTreeResponse tree(UUID ownerId, UUID workspaceId, String path, Integer limit, String cursor) {
        return inspection.tree(access.accessibleWorkspace(ownerId, workspaceId), path, limit, cursor);
    }

    public WorkspaceFileResponse file(
            UUID ownerId, UUID workspaceId, String path, Integer startLine, Integer endLine) {
        if (path == null || path.isBlank()) {
            throw new WorkspaceInvalidPathException();
        }
        return inspection.file(access.accessibleWorkspace(ownerId, workspaceId), path, startLine, endLine);
    }

    /**
     * Resolves a Workspace owned by the caller for use as a Knowledge Vault scope target. Preserved for
     * the existing Knowledge consumer; unlike the execution paths, it does not require a binding.
     */
    @Transactional(readOnly = true)
    public Workspace ownedWorkspace(UUID ownerId, UUID workspaceId) {
        return workspaces.findByIdAndOwnerId(workspaceId, ownerId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private void bindManaged(Workspace workspace, WorkspaceAccessMode accessMode) {
        if (!storage.isManagedRootAvailable()) {
            throw new WorkspaceUnavailableException("Managed workspace storage is unavailable");
        }
        workspace.bind(WorkspaceStorageType.MANAGED, null, accessMode);
        try {
            Files.createDirectories(pathResolver.workspaceRoot(workspace));
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException("Managed workspace directory could not be created");
        }
    }

    private void bindMounted(NexoUserPrincipal principal, Workspace workspace, BindWorkspaceRequest request) {
        if (principal.role() != UserRole.OWNER) {
            throw new WorkspaceUnavailableException("Only the owner may mount an existing project");
        }
        if (!storage.importRootAvailable()) {
            throw new WorkspaceUnavailableException("Mounted workspaces are unavailable on this server");
        }
        if (request.relativePath() == null || request.relativePath().isBlank()) {
            throw new WorkspaceInvalidPathException();
        }
        workspace.bind(WorkspaceStorageType.MOUNTED, request.relativePath().trim(), request.accessMode());
        Path root = pathResolver.workspaceRoot(workspace);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceUnavailableException("No project found at the requested import path");
        }
    }

    private WorkspaceResponse response(Workspace value) {
        return new WorkspaceResponse(
                value.getId(),
                value.getName(),
                value.getStorageType(),
                value.getAccessMode(),
                access.lightStatus(value),
                value.getRelativePath(),
                value.getLastScannedAt(),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }
}
