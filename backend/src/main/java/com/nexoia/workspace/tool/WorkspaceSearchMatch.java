package com.nexoia.workspace.tool;

/** One bounded search hit with a workspace-relative path and 1-based source line. */
public record WorkspaceSearchMatch(String path, int lineNumber, String excerpt) {}
