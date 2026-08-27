package com.nexoia.workspace.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Exact, server-processed replacement used to propose a surgical edit of one existing file. */
public record WorkspaceApplyPatchInput(
        String path,
        String oldString,
        String newString,
        @JsonProperty(required = false) Boolean replaceAll) {}
