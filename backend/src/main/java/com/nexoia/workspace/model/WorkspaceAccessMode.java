package com.nexoia.workspace.model;

/**
 * The strongest effect a Workspace permits, independent of the caller's permission profile. The
 * Permission Engine still resolves whether a given request may reach a capability; this only bounds
 * what the Workspace itself opts into. Reading never implies writing, and writing never implies
 * command execution.
 */
public enum WorkspaceAccessMode {

    /** Files may be listed, searched and read. No write, no command. */
    READ_ONLY,

    /** Read, plus proposed file changes that always pass through preview and approval. */
    WRITE_WITH_APPROVAL,

    /** Read and approved writes, plus allowlisted commands that pass through approval. */
    COMMANDS_WITH_APPROVAL;

    public boolean allowsWrite() {
        return this == WRITE_WITH_APPROVAL || this == COMMANDS_WITH_APPROVAL;
    }

    public boolean allowsCommands() {
        return this == COMMANDS_WITH_APPROVAL;
    }
}
