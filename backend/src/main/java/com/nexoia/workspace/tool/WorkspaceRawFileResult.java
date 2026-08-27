package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;

/** Internal server/runtime result containing raw text; this callback is never exposed to the model. */
public record WorkspaceRawFileResult(
        ToolExecutionStatus status,
        String path,
        String content,
        String sha256,
        String message) {}
