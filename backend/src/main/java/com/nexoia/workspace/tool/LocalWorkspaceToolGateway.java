package com.nexoia.workspace.tool;

import com.nexoia.device.exception.DeviceRuntimeException;
import com.nexoia.device.runtime.DeviceRuntimeSessionRegistry;
import com.nexoia.provider.dto.WorkspaceToolScope;
import java.time.Duration;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LocalWorkspaceToolGateway {

    private static final Duration TOOL_TIMEOUT = Duration.ofSeconds(45);

    private final DeviceRuntimeSessionRegistry sessions;
    private final ObjectMapper objectMapper;

    public LocalWorkspaceToolGateway(DeviceRuntimeSessionRegistry sessions, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    public WorkspaceListFilesResult listFiles(WorkspaceToolScope scope, WorkspaceListFilesInput input) {
        return execute(scope, "workspace.listFiles", input, WorkspaceListFilesResult.class);
    }

    public WorkspaceReadFileResult readFile(WorkspaceToolScope scope, WorkspaceReadFileInput input) {
        return execute(scope, "workspace.readFile", input, WorkspaceReadFileResult.class);
    }

    public WorkspaceWriteFileResult writeFile(WorkspaceToolScope scope, WorkspaceWriteFileInput input) {
        return execute(scope, "workspace.writeFile", input, WorkspaceWriteFileResult.class);
    }

    public WorkspaceSearchResult search(WorkspaceToolScope scope, WorkspaceSearchInput input) {
        return execute(scope, "workspace.search", input, WorkspaceSearchResult.class);
    }

    public WorkspaceGitStatusResult gitStatus(WorkspaceToolScope scope, WorkspaceProjectQueryInput input) {
        return execute(scope, "git.status", input, WorkspaceGitStatusResult.class);
    }

    public WorkspaceGitDiffResult gitDiff(WorkspaceToolScope scope, WorkspaceGitDiffInput input) {
        return execute(scope, "git.diff", input, WorkspaceGitDiffResult.class);
    }

    public WorkspaceInspectProjectResult inspectProject(
            WorkspaceToolScope scope,
            WorkspaceProjectQueryInput input) {
        return execute(scope, "workspace.inspect", input, WorkspaceInspectProjectResult.class);
    }

    private <T> T execute(WorkspaceToolScope scope, String method, Object input, Class<T> resultType) {
        JsonNode payload = objectMapper.createObjectNode()
                .put("localBindingId", scope.localBindingId())
                .set("input", objectMapper.valueToTree(input));
        JsonNode response = sessions.request(
                scope.deviceId(), scope.correlationId(), scope.assistantMessageId(),
                method, payload, TOOL_TIMEOUT);
        try {
            return objectMapper.treeToValue(response, resultType);
        } catch (JacksonException exception) {
            throw new DeviceRuntimeException("The local workspace returned an invalid tool result", exception);
        }
    }
}
