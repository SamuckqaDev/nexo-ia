package com.nexoia.workspace.tool;

/** Internal optimistic-concurrency input for a paired Desktop file deletion. */
public record WorkspaceDeleteFileRuntimeInput(String path, String expectedSha256) {}
