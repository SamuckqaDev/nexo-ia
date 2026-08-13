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
