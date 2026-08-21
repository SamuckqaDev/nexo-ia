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

## Initial implementation

The planned authoritative implementation supports a local root containing Markdown, YAML frontmatter, standard
Markdown links, `[[wikilinks]]`, tags, and attachments referenced by notes. It provides visible
ingestion status, incremental reindexing, citations that open the original file, and a relationship
graph. Obsidian Canvas and a dedicated Obsidian plugin remain later compatibility features.

The current frontend bridge is deliberately narrower. It keeps Vault drafts in a catalog partitioned
by authenticated user, shows bounded previews for explicitly selected Markdown, text, JSON, and CSV
files, and lets the user attach readable excerpts to Chat. Its interactive knowledge map shows Vault
membership, shared-term relationships, selection, and attached-source state. Attached excerpts are
sent inside the message to the selected provider as untrusted reference data; the map and excerpts
are not an index, retrieval result, citation, durable Vault ingestion, or a tool the model can use to
open an unattached source automatically. The contained Vault workspace keeps the collection library,
source list, and selected knowledge visible together on desktop; each surface owns its scrolling,
while narrow screens stack the same surfaces inside the Vault explorer. The relationship map opens as
a movable, resizable knowledge workbench with maximize and restore controls instead of consuming the
main explorer area; this is a client-side visualization and does not invoke a model. PDF and Office
selections keep metadata only until supported parsers exist.
