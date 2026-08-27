package com.nexoia.workspace.service;

import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceBinding;
import com.nexoia.workspace.tool.LocalWorkspaceToolGateway;
import com.nexoia.workspace.tool.WorkspaceListFilesInput;
import com.nexoia.workspace.tool.WorkspaceListFilesResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalWorkspaceBrowserService {

    private final WorkspaceService workspaces;
    private final WorkspaceBindingService bindings;
    private final LocalWorkspaceToolGateway gateway;

    public WorkspaceTreeResponse tree(
            UUID ownerId,
            UUID workspaceId,
            UUID bindingId,
            String path,
            Integer limit,
            String cursor) {
        Workspace workspace = workspaces.ownedWorkspace(ownerId, workspaceId);
        WorkspaceBinding binding = bindings.ownedBinding(ownerId, workspaceId, bindingId);
        WorkspaceToolScope scope = new WorkspaceToolScope(
                ownerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), workspaceId,
                workspace.getName(), workspace.getAccessMode(), true,
                binding.getId(), binding.getDeviceId(), binding.getLocalBindingId());
        WorkspaceListFilesResult result = gateway.listFiles(
                scope, new WorkspaceListFilesInput(path, limit, cursor));
        return new WorkspaceTreeResponse(
                result.path(), result.entries(), result.omissions(), result.truncated(), result.nextCursor());
    }
}
