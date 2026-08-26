package com.nexoia.workspace.model;

/**
 * The last observed availability of a Workspace's backing storage. Refreshed by an explicit scan;
 * never trusted from the client. Only {@code AVAILABLE} and {@code CHANGED} may serve reads, and a
 * {@code CHANGED} workspace still warns the user to refresh before acting on stale structure.
 */
public enum WorkspaceStatus {

    /** No path bound yet. */
    UNBOUND,

    /** Path resolved and readable at the last scan. */
    AVAILABLE,

    /** Bound but the path no longer resolves inside the configured root. */
    MISSING,

    /** Structure or Git HEAD moved since the last recorded fingerprint. */
    CHANGED,

    /** A mutating operation currently holds the Workspace. */
    LOCKED,

    /** The path resolved but could not be inspected safely. */
    ERROR;

    public boolean isReadable() {
        return this == AVAILABLE || this == CHANGED;
    }
}
