package com.nexoia.workspace.tool;

/** Model-facing input for proposing one new bounded UTF-8 file. */
public record WorkspaceCreateFileInput(String path, String content) {}
