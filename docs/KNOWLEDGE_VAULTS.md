# Nexo Knowledge Vaults

## Purpose

A Knowledge Vault is Nexo IA's portable, local-first unit for organizing interlinked knowledge and
making it available to RAG. It contains human-readable source material and explicit relationships;
PostgreSQL and `pgvector` contain the derived operational index.

An Obsidian vault may be used as a Nexo Vault because both can use ordinary Markdown, wikilinks,
frontmatter, tags, and local attachments. Obsidian is a compatible editor, not a required database,
runtime, or product dependency.

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

The first implementation supports a local root containing Markdown, YAML frontmatter, standard
Markdown links, `[[wikilinks]]`, tags, and attachments referenced by notes. It provides visible
ingestion status, incremental reindexing, citations that open the original file, and a relationship
graph. Obsidian Canvas and a dedicated Obsidian plugin remain later compatibility features.
