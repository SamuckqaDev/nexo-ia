# Decision log

This file indexes Nexo IA's architectural decisions. Relevant decisions must record the problem, the
options considered, the selected approach, and its consequences.

## D-001 — Create Nexo IA as an independent project

- **Status:** accepted
- **Context:** Avento has accumulated many capabilities and infrastructure components. The new
  project must support progressive study of its concepts.
- **Decision:** build Nexo IA from scratch in `Documents/projects/nexo-ia` without directly modifying or
  copying Avento's code.
- **Consequence:** concepts may be compared with Avento, but each implementation must justify its own
  architecture.

## D-002 — Document before selecting the technology stack

- **Status:** accepted
- **Context:** selecting technologies too early may automatically reproduce the previous project's
  architecture.
- **Decision:** define the identity, goals, principles, scope, and roadmap before creating the code
  structure.
- **Consequence:** the technology stack will be the next formal decision and must consider learning,
  simplicity, testing, and evolution.

## D-003 — Use Java and TypeScript as the primary languages

- **Status:** accepted
- **Context:** the creator wants to deepen Java knowledge while studying AI systems. Changing the
  backend language is not necessary to achieve a meaningful architectural and learning contrast with
  Avento.
- **Decision:** use Java 25 LTS for the backend and agent core, Spring Boot and Spring AI as selected
  adapters, and React with TypeScript for the interface.
- **Consequence:** Nexo IA can reuse the creator's Java experience while focusing its new learning on
  progressive architecture, explicit agent behavior, evaluation, and reliable permission control.
- **Details:** see [Technology stack](TECH_STACK.md).

## D-006 — Start on Java 25 LTS and the Spring 4 generation

- **Status:** accepted
- **Context:** Java 25 is the current LTS release, Spring Boot 4.1 supports Java through version 26,
  and Spring AI 2.0 documents compatibility with Spring Boot 4.0 and 4.1. Recent Baeldung examples
  confirm Spring AI 2.0 patterns on Spring Boot 4, although many retain Java 21 for compatibility.
- **Decision:** baseline the project on Java 25 LTS, Spring Boot 4.1.x, Spring AI 2.0.x, and Maven
  3.9.x through the wrapper. Do not enable Java preview features.
- **Consequence:** Nexo IA begins on the current LTS and framework generation. Dependency patch
  upgrades remain controlled and compatibility-tested.
- **Learning:** see [PILL-001](../pills/PILL-001-java-25-lts-baseline.md) and
  [PILL-002](../pills/PILL-002-compatible-spring-ai-stack.md).

## D-004 — Use PostgreSQL with pgvector as the initial vector store

- **Status:** accepted
- **Context:** Nexo IA needs relational ownership, document lifecycle, metadata filters, citations,
  permissions, and semantic retrieval. Redis Vector Search is capable, but would introduce a second
  operational data system before measurements justify it.
- **Decision:** store the first RAG corpus and embeddings in PostgreSQL with `pgvector`; use HNSW only
  when corpus size and benchmarks justify approximate search.
- **Consequence:** persistence and vector retrieval share transactions, backups, filters, and one
  operational model. Redis remains available as a measured later optimization rather than a default.

## D-005 — Include image generation as a post-core capability

- **Status:** accepted
- **Context:** image generation belongs to the intended assistant experience but should not delay the
  foundational chat, RAG, permission, MCP, and agent lessons.
- **Decision:** add provider-independent asynchronous image generation in a later phase, beginning
  with a local ComfyUI adapter and preserving complete provenance.
- **Consequence:** image generation is part of the product roadmap but remains isolated from the core
  agent architecture and from remote-provider assumptions.

## D-007 — Support governed computer control on Linux, Windows, and macOS

- **Status:** accepted
- **Context:** Nexo IA must be able to perform useful work on the user's computer regardless of the
  supported desktop operating system. Commands, paths, permissions, process models, application APIs,
  and desktop automation mechanisms differ significantly across platforms.
- **Decision:** define platform-neutral capability contracts and implement explicit Linux, Windows,
  and macOS adapters. Route every effect through the Permission Engine and expose capability support
  before execution.
- **Consequence:** core agent behavior remains portable while system integration is isolated and
  testable. Cross-platform releases require a real test matrix, and unsupported operations fail
  explicitly instead of being improvised by the model.

## D-008 — Organize RAG sources as Nexo Knowledge Vaults

- **Status:** accepted
- **Context:** flat document collections do not express the links, tags, properties, provenance, and
  project relationships needed for an interlinked personal and technical knowledge system.
- **Decision:** make Knowledge Vaults the product unit for RAG sources. Vault files remain portable
  and human-readable; PostgreSQL and `pgvector` store a rebuildable index. Support common Obsidian
  conventions without making Obsidian a required dependency.
- **Consequence:** retrieval can combine text, embeddings, metadata, and explicit relationships while
  preserving original citations. Vault, Workspace, Project, Memory, and Conversation remain separate
  concepts with independent permissions.

## D-009 — Require plans before consequential multi-step execution

- **Status:** accepted
- **Context:** large objectives overload a single model call, obscure progress, mix unrelated context,
  and make permissions difficult to understand or audit.
- **Decision:** require a visible execution plan before multi-step, long-running, high-impact,
  destructive, or security-sensitive effects. Decompose Goals into persisted milestones and bounded
  tasks, execute ready tasks incrementally, and verify declared evidence before completion.
- **Consequence:** plan approval and capability permission are separate decisions. Replanning is
  visible and cannot expand scope, risk, budget, targets, or permissions without a new decision.

## D-010 — Design for organizations and teams from the domain foundation

- **Status:** accepted
- **Decision:** model organizations, users, teams, ownership, sharing, policies, usage, and audit from
  the beginning while keeping the first deployment a modular monolith.
- **Consequence:** single-person installation remains simple, but personal data isolation and team
  administration do not require a future domain rewrite.

## D-011 — Separate central orchestration from endpoint execution

- **Status:** accepted
- **Decision:** allow Nexo Server and models to run locally or on central infrastructure such as a
  DGX, while a paired Nexo Companion performs authorized effects on a user's device.
- **Consequence:** processing and execution locations are recorded separately. Browser access alone
  never grants endpoint control, and every device has independent identity, policy, approval, health,
  revocation, inventory, and audit.

## D-012 — Prefer free and open tools through governed native or MCP integrations

- **Status:** accepted
- **Decision:** prefer maintained local and open-source tools, using native capabilities for core
  functions and MCP for reusable independent integrations. Build a project-owned MCP server when a
  lawful, stable CLI, API, or SDK exists and the integration provides measurable value.
- **Consequence:** MCP never bypasses licenses, authentication, quotas, or prices. Paid tools and
  remote providers remain optional and explicit.

## D-013 — Isolate context and reauthorize every Skill dependency

- **Status:** accepted
- **Decision:** assemble context for one authenticated principal and run using versioned, authorized,
  minimal resources. Give Skills explicit ownership and scopes, but no inherited permissions; resolve
  their dependencies again for the current principal.
- **Consequence:** sharing a Skill or Project never shares its author's Vaults, memories, Workspaces,
  secrets, providers, devices, or historical context. Context and cache isolation become
  release-blocking security requirements.

## D-014 — Permit governed Project database mutations

- **Status:** accepted
- **Decision:** support inspection, data mutation, schema evolution, migrations, backup, restore, and
  administration through granular, environment-specific database capabilities and a project-owned
  Database Safety Engine. Keep Project databases separate from Nexo IA persistence.
- **Consequence:** read-only remains the default but is not the product limit. Material changes require
  impact preview, an appropriate verified recovery method, explicit approval, database-aware safe
  execution, post-change validation, and complete audit. Unsupported safety guarantees block the
  operation instead of being improvised.

## D-015 — Freeze release 0.1 as the multi-user local chat foundation

- **Status:** accepted
- **Decision:** deliver Owner bootstrap, a default organization, managed members, revocable local
  sessions, organization-owned Ollama configuration, isolated persistent conversations, SSE streaming
  and cancellation, usage attribution, audit, and the minimum administration interface as release
  `0.1`. Use Maven Wrapper, PostgreSQL, web-first React, and `styled-components` with design tokens.
- **Consequence:** RAG, Memory, tools, databases, MCP, Agent Mode, Skills, Companion, Cowork,
  automations, images, and remote providers cannot enter `0.1` without formal change control. The
  release must prove two-user isolation and the security and quality gates in
  [MVP and release strategy](MVP_AND_RELEASE_STRATEGY.md).

## D-016 — Lock the reproducible release 0.1 stack

- **Status:** accepted
- **Decision:** scaffold release `0.1` with Java 25, Spring Boot 4.1.x, Spring AI 2.0.x, Maven
  Wrapper 3.9.x, PostgreSQL 18, Node 24 LTS, React 19.2, TypeScript 6.0, and Vite 8.1. Use Spring MVC
  with SSE, Spring Security, server-tracked JWT/refresh sessions, JPA, Flyway, Testcontainers, npm, and a committed
  lockfile. Pin exact resolved versions and container images in executable project files.
- **Consequence:** the stack is current, compatible, and reproducible without relying on a developer's
  global Maven or unsupported Node installation. See [Accepted stack baseline](STACK_BASELINE.md).

## D-017 — Use one application database until measured evidence requires another

- **Status:** accepted
- **Decision:** use PostgreSQL as the only release `0.1` application datastore, add `pgvector` only
  when the Knowledge/RAG release begins, and introduce neither Redis nor MongoDB without a measured
  capability or operational requirement.
- **Consequence:** Nexo IA avoids premature distributed state while retaining relational, JSON,
  full-text, and vector evolution paths. See [PILL-005](../pills/PILL-005-one-database-until-evidence.md).

## D-018 — Develop on Silverblue through Toolbx and host rootless Podman

- **Status:** accepted
- **Decision:** keep Java 25 and Node 24 tooling in a dedicated current Toolbx, use Maven/npm project
  locks, run PostgreSQL and Testcontainers through the host rootless Podman socket, and keep Ollama on
  the host for GPU access.
- **Consequence:** the immutable host remains clean and GPU inference stays native. Socket access and
  the host Ollama endpoint become explicit security boundaries and setup checks.

## D-019 — Publish four platform distribution profiles from one build

- **Status:** accepted
- **Decision:** publish Fedora Silverblue, conventional Linux, Windows, and macOS distribution
  profiles from the same versioned backend, frontend, PostgreSQL migrations, OCI images, and Compose
  contract. Keep Ollama host-native by default. Isolate platform differences in installation,
  networking, lifecycle, and the later native Companion.
- **Consequence:** Nexo IA avoids four application forks while providing platform-specific setup and
  verification. A profile is supported only after real-host install, upgrade, backup, restore,
  networking, security, and architecture tests pass. See
  [Cross-platform build and distribution profiles](DISTRIBUTION_BUILDS.md).

## D-020 — Use revocable browser token sessions with refresh rotation

- **Status:** accepted
- **Decision:** send a five-minute access JWT only in an `HttpOnly` cookie and a 30-day opaque
  refresh token in a narrower `HttpOnly` cookie. Store only refresh hashes, rotate on every refresh,
  track the current access `jti`, and revalidate the server-side session for every protected request.
- **Consequence:** logout, disablement, administrative revocation, and refresh replay can invalidate
  access before JWT expiry. The database records controlled IP, user-agent, expiry, rotation,
  revocation, and access-event metadata, but never raw tokens. CSRF protection remains required
  because browsers attach authentication cookies automatically.
