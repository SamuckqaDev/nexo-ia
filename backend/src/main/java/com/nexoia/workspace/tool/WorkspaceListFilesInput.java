package com.nexoia.workspace.tool;

/** Model-facing input for {@code workspace_list_files}. Path is workspace-relative; empty means root. */
public record WorkspaceListFilesInput(String path, Integer limit, String cursor) {

    public WorkspaceListFilesInput {
        path = path == null ? null : path.trim();
        if ("/".equals(path) || ".".equals(path)) {
            path = "";
        }
    }
}
