# Nexo IA — Claude Code Implementation Plan: Knowledge Vault and RAG 0.2

## Mission

Implement the first production-shaped Knowledge Vault and retrieval increment for Nexo IA. The
feature must organize user knowledge by owner, organization, project/workspace, team, and scope;
ingest bounded local sources; retrieve relevant passages; expose provenance in Chat; and enforce
authorization before any passage reaches a model provider.

This plan is for Claude Code. Execute it only from `feat/project-scaffold`, using the mandatory
Nexo Skill at `skills/build-nexo/SKILL.md` and every reference listed there.

## Current reality and boundaries

The current Skills and Workspace surfaces are partly client-local previews. Do not treat Zustand,
IndexedDB, or a prompt prefix as an authorization boundary. The RAG increment must move durable
knowledge ownership and permission decisions to the Java backend and PostgreSQL.

Do not implement in this increment:

- Agent Runtime or autonomous execution;
- MCP discovery or MCP execution;
- filesystem write/command execution;
- image generation;
- remote provider secret storage;
- memory learning across chats;
- arbitrary web crawling;
- silent ingestion of a user's whole disk;
- exposing raw file paths, secrets, or complete source documents to the model.

## Required execution rules

1. Read the Nexo Skill and all five referenced standards before editing.
2. Inspect existing modules, migrations, BaseResponse, GlobalExceptionHandler, GenericMapper,
   provider boundary, workspace contracts, Vault preview code, and Chat context assembly.
3. Work by business module. DTOs are separate top-level records in thematic folders; entities are
   never returned by controllers.
4. Keep controllers declarative. Services own authorization, transactions, ingestion, chunking,
   retrieval, mapping, and orchestration.
5. Use Lombok where it removes meaningful boilerplate, records for DTOs, Spring Data repositories,
   the existing generic mapper, personalized exceptions, and the existing BaseResponse contract.
6. Use Axios/TanStack Query/React Hook Form/Zod/styled-components conventions in the frontend.
   Keep types in module `types` folders and imports named, never namespace `S` imports.
7. Use cookies and the existing CSRF flow. Never add bearer tokens or store credentials in browser
   storage.
8. Use `.then()/.catch()/.finally()` in frontend code unless a library contract requires otherwise.
9. Preserve unrelated user changes and never edit or commit `main`.
10. Make small Conventional Commits and report every test and known limitation.

## Phase 0 — Baseline and design lock

Before coding, inspect and document:

- existing `vault` frontend module and its source metadata behavior;
- workspace owner and active workspace contracts;
- organization/team/user models and authorization patterns;
- PostgreSQL version and whether `pgvector` is available in the current image;
- current Java/Spring AI dependencies and the provider-neutral chat boundary;
- current token/context budget service;
- existing audit model and action enums.

Create a short decision record before implementation:

- embedding provider strategy for local-first development (default: Ollama embedding endpoint,
  configurable and never required for ordinary chat);
- vector dimensions and model identifier stored with every embedding;
- chunk size, overlap, source limits, MIME policy, and maximum extracted bytes;
- retrieval top-k, minimum score, context budget, and citation format;
- fallback behavior when embeddings are unavailable (safe lexical retrieval or explicit no-results,
  never an invented answer);
- how a local browser-selected workspace source is uploaded/authorized without leaking absolute
  paths.

Commit: `docs(rag): lock knowledge and retrieval decisions`.

## Phase 1 — Backend Knowledge Vault domain

Create a business-oriented module, for example:

```text
backend/src/main/java/com/nexoia/knowledge/
  vault/
    controller/
    service/
    repository/
    model/
    dto/
      vault/
      source/
      chunk/
      retrieval/
    exception/
  ingestion/
  retrieval/
  embedding/
```

Model the minimum durable entities:

- `KnowledgeVault`: owner, organization, name, description, scope, project/workspace reference,
  team reference, status, created/updated timestamps;
- `KnowledgeSource`: vault, source kind, display name, safe logical identifier, MIME type, content
  hash, ingestion status, error code, metadata, timestamps;
- `KnowledgeChunk`: source, ordinal, bounded text, token estimate, embedding vector, embedding model,
  dimensions, metadata JSON, timestamps;
- explicit access/grant records only if existing organization/team contracts support them; otherwise
  define the contract and keep unsupported sharing disabled.

Rules:

- never persist absolute browser paths or provider credentials;
- never expose raw embeddings through an API;
- scope must be explicit and validated (`personal`, `project`, `workspace`, `team`, `organization`);
- project/workspace/team targets are required for their scopes;
- owner and organization isolation is mandatory on every repository query;
- soft-delete or status transition must prevent retrieval from removed sources.

Add Flyway migrations and indexes for owner, organization, scope, source status, content hash, and
vector search. If pgvector cannot be enabled in the current PostgreSQL image, stop before silently
substituting an unbounded in-memory vector store; document the exact local image decision.

Commit: `feat(knowledge): add scoped vault source and chunk domain`.

## Phase 2 — API contracts and authorization

Add separate request/response records under the thematic DTO folders:

- create/update/list vault;
- register source metadata;
- upload/import source content through an explicit bounded endpoint;
- ingestion status;
- retrieval preview with citations and scores (admin/debug-safe fields only);
- delete/archive source.

Endpoints should include:

- `GET/POST/PUT/DELETE /api/v1/knowledge/vaults`;
- `GET/POST/DELETE /api/v1/knowledge/vaults/{vaultId}/sources`;
- `GET /api/v1/knowledge/sources/{sourceId}/ingestion`;
- a service-owned retrieval contract used by Chat, not a controller-to-controller call.

Use `ResponseEntity<BaseResponse<T>>` for ordinary endpoints and typed SSE only if ingestion is
explicitly streamed. Add personalized exceptions and grouped `@ExceptionHandler` mappings for:

- vault/source not found;
- scope target not found;
- forbidden knowledge access;
- unsupported source type;
- source too large;
- ingestion failure;
- embedding provider unavailable;
- retrieval budget exceeded.

Commit: `feat(knowledge): expose authorized vault contracts`.

## Phase 3 — Ingestion pipeline

Implement a service pipeline:

```text
authorize source -> validate type/size -> normalize text -> chunk -> embed -> persist -> audit
```

Initial supported formats:

- Markdown;
- plain text;
- JSON with bounded pretty/field-aware extraction;
- CSV with bounded row/column handling.

Keep PDF/Office as metadata-only or an explicit unsupported status until a maintained extractor is
selected and tested. Do not upload a whole directory implicitly. Every imported source must be
explicitly selected or come from an already-authorized workspace snapshot.

Requirements:

- deterministic content hash and idempotent re-ingestion;
- bounded extraction and chunk counts;
- no raw provider response or secret in logs;
- failure status persisted with a safe public message and correlation ID;
- audit source registration, ingestion start, completion, failure, and deletion;
- asynchronous execution only if the existing task infrastructure can own lifecycle and retry;
  otherwise implement a bounded synchronous service first.

Commit: `feat(knowledge): implement bounded source ingestion`.

## Phase 4 — Embedding and retrieval boundary

Create a real provider boundary only where multiple implementations are needed:

- local Ollama embeddings first;
- a deterministic test embedding implementation;
- future remote adapters remain unsupported until credential policy exists.

Implement retrieval service behavior:

1. authenticate principal and resolve organization/project/workspace/team scope;
2. filter vaults and sources by authorization before vector search;
3. embed the query using the configured embedding model;
4. perform vector search with lexical/metadata constraints;
5. rank and cap results by score, source count, and token budget;
6. return citations containing safe vault/source/chunk labels, never absolute paths or secrets;
7. return an explicit empty result when confidence is insufficient.

Add tests for cross-user, cross-organization, project, workspace, and team isolation. A result from
an unauthorized source must never reach the model request assembler.

Commit: `feat(knowledge): add authorized pgvector retrieval`.

## Phase 5 — Chat integration

Integrate retrieval in the backend Chat service, not in the controller and not only by client prompt
prefix:

- accept a typed retrieval mode/knowledge selection in the chat request;
- resolve the user's active conversation/project/workspace scope server-side;
- retrieve bounded excerpts before model invocation;
- assemble a typed context envelope containing excerpts and citations;
- mark retrieved text as untrusted reference context;
- preserve the existing model token budget and SSE lifecycle;
- emit a typed retrieval event only if it adds clear UI value; otherwise return citations in a safe
  completion metadata event;
- never persist private model reasoning; persist only user message, answer, citations, usage, and
  audit metadata.

The frontend must show:

- Knowledge/Vault context selector;
- loading, empty, unavailable, and permission-denied states;
- source chips/citations on the answer;
- a context inspector that never displays secrets or raw absolute paths;
- a clear label when an answer used no retrieved sources.

Commit: `feat(chat): integrate scoped knowledge retrieval`.

## Phase 6 — Frontend Vault and scope UX

Refactor the existing Vault preview into the backend-backed flow:

- list vaults for the authenticated user;
- create a vault with scope and required project/workspace/team target;
- add sources through explicit selection;
- show ingestion status and retry action;
- show source hash, type, size, and safe logical name;
- keep project/workspace selection consistent with Chat and Skills;
- never claim that a local-only browser handle is a server-readable file.

Use module hooks, Axios API functions, Zod response schemas, React Hook Form, and named styled
components. Keep all server state in TanStack Query and transient UI state local.

Commit: `feat(vaults): connect scoped vault management UI`.

## Phase 7 — Verification and delivery

Run and record:

- backend `./mvnw -Dexcluded.test.groups=ollama,docker verify`;
- frontend `npm test -- --run`;
- frontend `npm run build`;
- `git diff --check`;
- migration validation against empty PostgreSQL;
- pgvector availability check;
- explicit Ollama embedding smoke test when available;
- Compose runtime check with PostgreSQL and Ollama;
- manual smoke test: create vault → add source → wait ingestion → open Chat → select vault → ask
  question → expand citations → verify source isolation with a second user.

Update `docs/IMPLEMENTATION_STATUS.md`, add a RAG section to the roadmap if needed, and document
all unavailable capabilities instead of presenting previews as production functionality.

Final commits should be small and Conventional Commit compliant. Do not push to GitHub in this
execution. End with a report containing changed files, commits, tests, migration status, provider
requirements, and unresolved blockers.

## Definition of done

- A user can create a scoped Knowledge Vault and explicitly add bounded supported sources.
- Ingestion is idempotent, observable, auditable, and safe on failure.
- Retrieved chunks are filtered by authenticated ownership and scope before model invocation.
- Chat answers show safe citations and do not leak paths, secrets, or unauthorized knowledge.
- Project/workspace/team scope is represented by real durable identifiers, not only prompt text or
  local UI state.
- All normal APIs follow BaseResponse, DTO, exception, service, mapping, cookie, and frontend rules.
- Tests prove positive behavior and negative isolation boundaries.
