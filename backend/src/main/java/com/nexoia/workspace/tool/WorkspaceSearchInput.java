package com.nexoia.workspace.tool;

/** Literal text search input. Path is optional and always workspace-relative. */
public record WorkspaceSearchInput(String query, String path, Integer limit) {}
