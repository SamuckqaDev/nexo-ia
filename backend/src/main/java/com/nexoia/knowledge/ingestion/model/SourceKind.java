package com.nexoia.knowledge.ingestion.model;

/**
 * Only {@code UPLOAD} is implemented in this release — a browser reads a local file and posts its
 * bytes. Workspace-snapshot-based ingestion is out of scope: D-023 forbids reading file contents into
 * the frontend workspace snapshot, so no legitimate byte source exists for it yet.
 */
public enum SourceKind {
    UPLOAD,
    /** Knowledge the assistant appended through the governed {@code save_to_vault} tool. */
    AGENT
}
