package com.nexoia.workspace.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model-facing input for one bounded Workspace file write. Existing files require the SHA-256 from
 * a prior {@code workspace_read_file} call so stale or blind overwrites are rejected.
 */
public record WorkspaceWriteFileInput(
        String path,
        String content,
        @JsonProperty(required = false) String expectedSha256) {}
