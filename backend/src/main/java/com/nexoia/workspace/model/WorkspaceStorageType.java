package com.nexoia.workspace.model;

/**
 * Where a Workspace's files physically live, as far as the Nexo server is concerned.
 *
 * <ul>
 *   <li>{@code UNBOUND} — a name only, with no server-side path. Legacy records used purely as a
 *       Knowledge Vault scope target start here and cannot be read until explicitly bound.</li>
 *   <li>{@code MANAGED} — storage the Nexo server owns under its managed root, resolved only to
 *       {@code {managedRoot}/{ownerId}/{workspaceId}}.</li>
 *   <li>{@code MOUNTED} — an existing project under the server's configured import root, resolved
 *       only to {@code {importRoot}/{relativePath}}.</li>
 * </ul>
 */
public enum WorkspaceStorageType {
    UNBOUND,
    MANAGED,
    MOUNTED
}
