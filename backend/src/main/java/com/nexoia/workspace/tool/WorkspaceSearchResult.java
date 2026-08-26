package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;
import java.util.List;

/** Bounded literal search result over readable project text files. */
public record WorkspaceSearchResult(
        ToolExecutionStatus status,
        List<WorkspaceSearchMatch> matches,
        boolean truncated,
        String message) {}
