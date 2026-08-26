package com.nexoia.workspace.tool;

/**
 * Model-facing input for the no-argument project tools ({@code workspace_git_status},
 * {@code workspace_inspect_project}). The optional focus is descriptive only and never affects scope.
 */
public record WorkspaceProjectQueryInput(String focus) {}
