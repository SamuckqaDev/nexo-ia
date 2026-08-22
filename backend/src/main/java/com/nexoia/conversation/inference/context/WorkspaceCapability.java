package com.nexoia.conversation.inference.context;

/**
 * Whether a project Workspace is attached and whether the backend can actually act on it. Presence
 * without server-side access must not let the model claim it can read or change files.
 */
public record WorkspaceCapability(boolean present, String name, boolean serverSideAccess) {

    public static WorkspaceCapability none() {
        return new WorkspaceCapability(false, null, false);
    }
}
