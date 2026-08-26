package com.nexoia.workspace.dto;

/** Safe Git metadata read from the Workspace's {@code .git} directory: branch and short/long HEAD. */
public record WorkspaceGitSummary(String branch, String head, boolean detached) {}
