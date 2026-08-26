package com.nexoia.workspace.dto;

import java.util.List;

/**
 * A single, lazily-loaded directory listing. {@code path} is the Workspace-relative directory shown;
 * {@code nextCursor} is non-null when more entries remain to be fetched for the same directory.
 */
public record WorkspaceTreeResponse(
        String path,
        List<WorkspaceTreeEntryResponse> entries,
        List<WorkspaceOmissionResponse> omissions,
        boolean truncated,
        String nextCursor) {}
