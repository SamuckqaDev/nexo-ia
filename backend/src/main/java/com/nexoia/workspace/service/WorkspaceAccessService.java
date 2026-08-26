package com.nexoia.workspace.service;

import com.nexoia.workspace.exception.WorkspaceNotFoundException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Workspace isolation boundary: every server operation resolves the target through here so a user
 * can only ever reach a Workspace they own. It also computes a light availability status used by
 * listings; the full inspected status (including CHANGED) is produced by
 * {@link WorkspaceInspectionService}.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceAccessService {

    private final WorkspaceRepository workspaces;
    private final WorkspacePathResolver pathResolver;

    /** Resolves a Workspace owned by the caller, or 404 — foreign ownership is never distinguishable. */
    @Transactional(readOnly = true)
    public Workspace accessibleWorkspace(UUID userId, UUID workspaceId) {
        return workspaces.findByIdAndOwnerId(workspaceId, userId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    /**
     * A cheap status suitable for a listing: UNBOUND when there is no binding, AVAILABLE when the root
     * resolves to a readable directory, MISSING when it does not, ERROR when resolution itself fails.
     */
    public WorkspaceStatus lightStatus(Workspace workspace) {
        if (!workspace.isBound()) {
            return WorkspaceStatus.UNBOUND;
        }
        try {
            Path root = pathResolver.workspaceRoot(workspace);
            boolean readable = Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(root);
            return readable ? WorkspaceStatus.AVAILABLE : WorkspaceStatus.MISSING;
        } catch (RuntimeException exception) {
            return WorkspaceStatus.ERROR;
        }
    }
}
