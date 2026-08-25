package com.nexoia.permission.model;

/**
 * The progressive, cumulative permission tiers of the Nexo Permission Engine. Each level adds the
 * capability families of the levels below it. A request runs at exactly one effective level, and the
 * declaration order here is the authority for "greater or equal" comparisons.
 *
 * <p>Levels govern <em>capabilities</em> (effects the assistant may cause), never topics: content
 * permissiveness is a separate axis. See {@code docs/PERMISSION_PROFILES.md}.
 */
public enum UnlockLevel {

    /** Read-only inference: answer, explain, translate. No tools. */
    L0_OBSERVER,

    /** Grounded on the user's own data: knowledge read/write and personal memory. */
    L1_GROUNDED,

    /** Governed external read: visible planning and enabled read-only MCP tools. */
    L2_CONNECTED,

    /** Deferred. Reads files and repository metadata in an attached Workspace. */
    L3_WORKSPACE_READER,

    /** Deferred. Guarded writes (files, Git, write MCP), each behind preview and approval. */
    L4_BUILDER,

    /** Deferred. System and computer control, each behind fresh confirmation and OS consent. */
    L5_OPERATOR;

    /** Whether this level is at least {@code required}, i.e. it unlocks that tier's capabilities. */
    public boolean grants(UnlockLevel required) {
        return ordinal() >= required.ordinal();
    }

    /** The lower (more restrictive) of the two levels — the basis of effective-level resolution. */
    public UnlockLevel min(UnlockLevel other) {
        return ordinal() <= other.ordinal() ? this : other;
    }

    /** A human label for the capability envelope, for example {@code "L2 (Connected)"}. */
    public String label() {
        int separator = name().indexOf('_');
        String code = name().substring(0, separator);
        String title = name().substring(separator + 1).replace('_', ' ').toLowerCase();
        return code + " (" + Character.toUpperCase(title.charAt(0)) + title.substring(1) + ")";
    }
}
