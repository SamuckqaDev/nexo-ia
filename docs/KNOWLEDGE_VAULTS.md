# Nexo Knowledge Vaults

## Purpose

A Knowledge Vault is Nexo IA's portable, local-first unit for organizing interlinked knowledge and
making it available to RAG. It contains human-readable source material and explicit relationships;
PostgreSQL and `pgvector` contain the derived operational index.

An Obsidian vault may be used as a Nexo Vault because both can use ordinary Markdown, wikilinks,
frontmatter, tags, and local attachments. Obsidian is a compatible editor, not a required database,
runtime, or product dependency.

The retrieval mechanism behind a Vault — ingestion, embedding, `pgvector` search, the
authorization boundary, and the data model — is described in
[RAG and retrieval architecture](RAG_ARCHITECTURE.md).

## Conceptual model

```text
Knowledge Vault
  -> notes, documents, code, and attachments
  -> links, backlinks, tags, properties, and provenance
  -> parsing, normalization, and structure-aware chunks
  -> PostgreSQL metadata + pgvector embeddings
  -> textual, semantic, and relationship-aware retrieval
  -> grounded context with citations
```

The source files remain readable and useful without Nexo IA. The database index is disposable and
must be rebuildable from the Vault without losing source knowledge.

## Vault contents

A Vault may contain:

- Markdown notes, source code, supported documents, images, and attachments;
- Markdown links, `[[wikilinks]]`, backlinks, tags, and YAML frontmatter;
- manually authored relationships and model-suggested relationships approved by the user;
- source, author, authority, version, creation time, update time, and lifecycle metadata;
- Vault-level ingestion, embedding, retrieval, retention, and access policies;
- links to related Projects, Workspaces, Cowork sessions, Skills, and Learning Pills.

## Retrieval

Retrieval combines PostgreSQL full-text search, `pgvector` similarity, metadata filters, and explicit
Vault relationships. Relationship-aware expansion may consider linked notes, backlinks, shared tags,
hierarchy, project context, source authority, recency, and the active Cowork objective.

Every returned passage preserves Vault, source file, section, version, and relationship provenance.
Generated answers cite the original material and report insufficient or conflicting evidence.

## Boundaries

- A **Vault** organizes source knowledge and its relationships for retrieval.
- A **Workspace** is an authorized filesystem scope in which tools may operate.
- A **Project** groups product or engineering context, objectives, and resources.
- **Memory** stores explicit durable facts or preferences, not source documents.
- A **Conversation** stores messages and short-term interaction context.

The same directory may be a Workspace and a Vault source, but read, index, create, edit, move, and
delete permissions remain independent.

## Safety and portability

- Vault access is granted per root, subdirectory, and operation.
- Model-suggested links or metadata changes are proposals until an authorized workflow applies them.
- Source files are never replaced by generated chunks or embeddings.
- Secrets, ignored paths, private properties, and unsupported binaries are excluded according to the
  Vault policy before model or embedding access.
- Incremental ingestion reacts to create, update, move, and delete events while retaining auditable
  provenance.
- Export and index rebuilding do not require Obsidian or a proprietary file format.

## Current implementation

The authoritative Vault catalog, uploaded sources, normalized text, chunks, embeddings, and citations
live in PostgreSQL. The Vault page and Chat both read this authenticated backend catalog; selecting a
Vault in Chat sends its id to server-side retrieval instead of embedding a client-side preview in the
message. Markdown, text, JSON, and CSV are currently ingestible. PDF and Office files remain
metadata-only until supported parsers exist.

Uploaded and Agent-authored sources share the same strict frontend contract. The ingestion request
flushes its initial and terminal persistence state before returning, so Hibernate-managed timestamps
are always present. A source row is informational; removal is available only through its explicit
compact Remove action and a confirmation dialog, preventing an inspection click from silently
archiving knowledge.

`GET /api/v1/knowledge/graph` builds a bounded semantic view of the current user's real index. It
returns Vault, document, and chunk nodes; containment edges; and the strongest cross-document chunk
relationships whose cosine similarity is at least 0.58. Repository joins apply owner, archive, and
ingestion-status filters before graph data is loaded. Raw embedding vectors never leave the backend.
The response is capped at 24 Vaults, 96 sources, 160 chunks, 120 semantic edges, and three semantic
edges per chunk so opening the map cannot request an unbounded corpus.

The Vault page opens this data in a movable, resizable, maximizable Knowledge Workbench. The user can
pan with the viewport scrollbars, zoom, search, hide or show chunk detail, and inspect a bounded chunk
excerpt. When chunks are hidden, the frontend collapses their strongest relationships into
document-level links. This provides the Obsidian-style network view without introducing a separate
graph database.

Explicit Markdown links, `[[wikilinks]]`, backlinks, tags, frontmatter, incremental filesystem
reindexing, and user-approved model-suggested links remain later increments. Current semantic edges
are inferred from the existing embeddings and must not be presented as authored links.
