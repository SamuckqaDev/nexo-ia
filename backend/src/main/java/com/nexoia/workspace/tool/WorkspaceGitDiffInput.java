package com.nexoia.workspace.tool;

/** Requests a read-only Git diff for one explicit workspace-relative, non-sensitive path. */
public record WorkspaceGitDiffInput(String path) {}
