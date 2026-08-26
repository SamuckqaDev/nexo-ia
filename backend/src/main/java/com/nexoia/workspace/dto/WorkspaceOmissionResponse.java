package com.nexoia.workspace.dto;

/** A directory entry deliberately left out of a listing, with a safe reason (e.g. ignored, too deep). */
public record WorkspaceOmissionResponse(String name, String reason) {}
