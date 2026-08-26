package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.workspace.dto.WorkspaceOmissionResponse;
import com.nexoia.workspace.dto.WorkspaceTreeEntryResponse;
import java.util.List;

/** Bounded, model-facing directory listing containing workspace-relative paths only. */
public record WorkspaceListFilesResult(
        ToolExecutionStatus status,
        String path,
        List<WorkspaceTreeEntryResponse> entries,
        List<WorkspaceOmissionResponse> omissions,
        boolean truncated,
        String nextCursor,
        String message) {}
