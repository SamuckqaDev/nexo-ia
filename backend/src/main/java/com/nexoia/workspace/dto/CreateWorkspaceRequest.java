package com.nexoia.workspace.dto;

import com.nexoia.workspace.model.WorkspaceAccessMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank @Size(max = 160) String name,
        WorkspaceAccessMode accessMode) {

    public CreateWorkspaceRequest(String name) {
        this(name, null);
    }
}
