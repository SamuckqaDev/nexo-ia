package com.nexoia.workspace.tool;

/** Model-facing input for {@code workspace_read_file}. Lines are 1-based and optional. */
public record WorkspaceReadFileInput(String path, Integer startLine, Integer endLine) {}
