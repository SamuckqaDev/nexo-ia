package com.nexoia.workspace.dto;

import com.nexoia.workspace.model.WorkspaceBindingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterLocalWorkspaceBindingRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = "[A-Za-z0-9_-]+") String localBindingId,
        @NotBlank @Size(max = 240) String displayName,
        @NotNull WorkspaceBindingStatus status,
        @Size(max = 64) String structureFingerprint,
        @Size(max = 64) String gitHead,
        @Size(max = 240) String gitBranch) {}
