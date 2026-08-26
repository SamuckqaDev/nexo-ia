package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/** Request-local workspace callbacks and their persistable governed evidence. */
public record WorkspaceReadToolSession(
        List<ToolCallback> callbacks,
        List<ToolExecutionEvidence> evidence) {

    public WorkspaceReadToolSession {
        callbacks = List.copyOf(callbacks);
    }
}
