package com.nexoia.workspace.dto;

/**
 * A bounded, text-only file preview. {@code content} carries the requested (optionally line-ranged)
 * text; {@code truncated} is set when the file exceeded the returned range. Binary files are never
 * returned — the endpoint fails with a safe error instead.
 */
public record WorkspaceFileResponse(
        String path,
        String content,
        int startLine,
        int endLine,
        int totalLines,
        long sizeBytes,
        String sha256,
        boolean truncated) {}
