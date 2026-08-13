# Technology stack

## Status

**Java direction accepted; exact framework versions proposed.**

Nexo IA will remain a Java project. The learning contrast with Avento will come from progressive
delivery, clearer boundaries, and explicit implementations—not from changing the primary language.

## Decision criteria

The stack must:

1. keep Java as the primary backend and agent language;
2. make LLM, RAG, tool, permission, MCP, and agent concepts visible;
3. support streaming, human approval, and long-running background work;
4. work locally with Ollama and local data;
5. enforce typed contracts and automated tests;
6. avoid distributed infrastructure until a demonstrated need appears;
7. provide a path to a desktop application without coupling the core to the desktop shell.

## Proposed stack

### Languages and runtime

- Java 25 LTS for the backend, agent core, tools, RAG, MCP, and background execution.
- TypeScript for the web interface.
- Node.js LTS for frontend tooling only.

Java 25 is the newest LTS release available at the start of the project. The selected Spring Boot
line supports it. Preview features remain disabled so the baseline is modern without depending on
experimental language contracts.

#### Why Java 25 instead of Java 21

Both releases are LTS and Java 21 remains a valid runtime for many applications. Nexo IA selects
Java 25 because it is the current LTS for a new project and includes four releases of finalized
language, concurrency, runtime, and observability improvements:

| Area | Java 21 | Java 25 and changes delivered since 21 |
|---|---|---|
| Virtual threads | Final, but some synchronized blocking can pin carrier threads | Ordinary synchronized blocking no longer has the previous pinning limitation |
| Scoped Values | Preview | Final API for bounded immutable context propagation |
| Structured Concurrency | Preview | Still preview and therefore excluded from Nexo IA production code |
| Stream processing | Standard Stream API | Final Stream Gatherers for custom intermediate operations |
| Native integration | Foreign Function and Memory API in preview | Foreign Function and Memory API final since Java 22 |
| Bytecode tooling | Third-party APIs normally required | Final Class-File API since Java 24 |
| Language | Java 21 language baseline | Unnamed variables, flexible constructor bodies, module imports, and Markdown documentation comments |
| Runtime | Earlier GC, JFR, and startup capabilities | Generational collectors, compact object headers, AOT, and JFR improvements |
| Security Manager | Deprecated and unusable as a modern security strategy | Permanently disabled since Java 24 |
| Platform | Includes the 32-bit x86 port | 32-bit x86 port removed |

The newer JDK does not make model inference faster by itself and does not sandbox tools. Nexo IA
will use Java 25 without preview features and adopt individual capabilities only when a measured or
architectural need justifies them. In particular, the Permission Engine relies on capability scopes,
process and operating-system boundaries, validation, timeouts, and auditing—not Security Manager.

See [PILL-004 — What Java 25 adds beyond Java 21](../pills/PILL-004-java-21-to-25.md) for the detailed
comparison and official OpenJDK sources.

### Repository and build

- Monorepo containing `backend`, `frontend`, and `docs`.
- Maven Wrapper as the only required Maven entry point.
- A single Spring Boot application organized as a modular monolith.
- Maven Enforcer to validate the Java and Maven versions.
- Spotless for formatting and Checkstyle or ArchUnit for enforceable architectural rules.
- Conventional commits and architecture decision records.

Nexo IA must not begin with ten Maven modules. Package boundaries come first. A package becomes a
separate build module only when a tested dependency boundary or independent lifecycle justifies it.

### Backend

- Spring Boot.
- Spring Web MVC with `StreamingResponseBody` or SSE for streaming agent events.
- Spring Validation and immutable Java records at external boundaries.
- Spring Data JPA for persistence.
- Flyway for schema migrations.
- Jackson for JSON contracts.
- Micrometer and Spring Boot Actuator for metrics and health information.
- Logback with structured JSON output and correlation identifiers.

Spring Boot is the application framework, not the domain architecture. Agent, permission, tool, and
knowledge behavior must remain testable without HTTP, JPA, Ollama, or an MCP server.

### Spring AI and agent core

- Use Spring AI for provider adapters, model abstractions, embeddings, vector stores, and MCP
  integration where its contracts help rather than hide the lesson.
- Use Spring AI's Ollama integration for the default local chat and embedding models.
- Implement Nexo IA's first context assembler, agent loop, tool registry, execution limits, and
  Permission Engine in project-owned code.
- Keep a project-owned provider boundary so tools, permissions, and agent state do not depend on a
  specific `ChatModel` implementation.
- Use JSON Schema-compatible contracts for tool input and structured results.
- Use the official MCP Java SDK through Spring AI's MCP integration where appropriate.
- Do not add LangChain4j beside Spring AI in the foundation. Re-evaluate it later through an isolated
  experiment if it offers a capability we can measure.

This avoids two competing AI abstractions and lets us learn which responsibilities belong to Nexo IA.

### Frontend

- React with TypeScript.
- Vite for local development and production builds.
- TanStack Router for typed navigation.
- TanStack Query for server state and request lifecycle.
- Zod for runtime validation at selected untrusted boundaries.
- CSS Modules with design tokens for initial styling.
- Vitest and Testing Library for component and behavior tests.
- Playwright for critical user journeys.

Use native `fetch` and Server-Sent Events for chat output and run timelines. Add WebSockets only when
a concrete bidirectional real-time requirement cannot be met by REST plus SSE.

### HTTP and event contracts

- OpenAPI generated from the Java backend is the source of truth for REST contracts.
- Generate TypeScript client types instead of maintaining duplicate DTOs manually.
- Version agent event schemas independently from ordinary REST responses.
- Use a stable error contract with code, message, trace identifier, and field errors.
- Do not expose JPA entities through HTTP or agent tool boundaries.

### Persistence evolution

#### First chat milestone

- H2 only for fast automated repository tests.
- PostgreSQL for the real local application from the first persistent milestone.
- Local filesystem for explicitly authorized documents, artifacts, and backups.

Using PostgreSQL early avoids a later SQLite-to-PostgreSQL semantic migration while H2 keeps focused
tests fast. Repository integration tests must still run against PostgreSQL with Testcontainers.

#### RAG

- Add the `pgvector` extension when semantic retrieval begins.
- Keep document metadata, ingestion versions, chunks, embeddings, and citations explicitly modeled.
- Use Ollama embeddings for the default local path.
- Build the pipeline visibly: load, normalize, split, embed, store, retrieve, rerank, and answer.
- Add a retrieval evaluation set before tuning chunk sizes or similarity thresholds.

`pgvector` is the accepted initial vector store. It keeps relational metadata, ownership, ingestion
state, citations, and embeddings in one transactional system and supports exact search, HNSW,
IVFFlat, metadata filtering, and hybrid search with PostgreSQL full-text search.

Redis Vector Search remains a later optimization candidate. It should be introduced only if measured
latency, throughput, transient-memory, caching, stream, or multi-worker requirements justify a second
data system. A benchmark must compare recall, filtered-query behavior, latency, memory, restart
recovery, and operational cost using Nexo IA's own corpus.

Do not introduce Redis as a default dependency. Add it only if measurements prove a need for
cross-process streams, caching, or queue distribution.

### Scheduled work

- Use Spring's scheduling support only to wake the scheduler, not as the source of truth.
- Persist automation definitions and occurrences in PostgreSQL.
- Claim due occurrences transactionally with leases and idempotency keys.
- Store timezone, recurrence rule, timeout, retry, concurrency, and permission policy explicitly.
- Start with one in-process worker using virtual threads for suitable I/O-bound runs.
- Consider Quartz only when its calendar and clustering capabilities are demonstrably required.
- Consider an external workflow engine only when recovery and orchestration exceed the local design.

### Image generation

- Define project-owned image request, job, progress, artifact, and provenance contracts.
- Prefer a local ComfyUI adapter for the first implementation.
- Use Spring AI's `ImageModel` abstraction only for providers it supports cleanly and keep it behind
  the same Nexo IA provider boundary.
- Persist job state and metadata in PostgreSQL while storing generated binary files on disk.
- Use asynchronous, cancellable jobs with explicit resource, output-path, timeout, and concurrency
  limits.
- Never send prompts or source images to a remote provider without explicit opt-in.

### Security and permissions

- Bind to loopback by default.
- Begin as a single-user local application without remote registration.
- Keep Spring Security even locally so HTTP boundaries, CSRF, and future remote access are explicit.
- Canonicalize and authorize workspace roots before filesystem access.
- Run every native and MCP tool through the same capability-based Permission Engine.
- Separate skill instructions from permissions.
- Store secrets outside configuration committed to Git.
- Redact credentials and sensitive values from prompts, events, and logs.
- Execute risky commands through a constrained process adapter with workspace, environment, timeout,
  output, and cancellation limits.

### Testing

- JUnit 5, AssertJ, and Mockito for focused unit tests.
- Spring Boot Test for application wiring tests.
- Testcontainers for PostgreSQL, pgvector, and integration dependencies.
- WireMock or MockWebServer for deterministic Ollama/provider protocol tests.
- A fake MCP server for lifecycle and contract tests.
- jqwik for property-based tests of paths, permission rules, parsers, recurrence, and loop limits.
- ArchUnit for package dependency rules.
- Vitest and Testing Library for the frontend.
- Playwright for chat, approval, Cowork, and automation journeys.
- Real Ollama tests remain explicitly tagged and opt-in.

### Desktop packaging

- Start as a loopback web application.
- Evaluate Tauri after the web experience and Java process lifecycle are stable.
- Keep the desktop shell as an adapter; the backend and domain model cannot depend on it.

## Package architecture

Use capability-oriented packages with internal layers rather than one global controller/service/
repository hierarchy:

```text
com.nexoia
  shared
  conversation
    domain
    application
    infrastructure
    web
  model
  agent
  permission
  tool
  workspace
  knowledge
  mcp
  skill
  cowork
  automation
  observability
```

Dependencies point inward: web and infrastructure adapt application use cases; domain code does not
import Spring, JPA, Ollama, or MCP transport classes unless a framework integration has a narrowly
justified boundary.

## Baeldung material reviewed

The Baeldung website itself returned a Cloudflare challenge to automated requests. Its public
`eugenp/tutorials` repository nevertheless exposes the accompanying Java examples. Relevant areas
include:

- Spring AI chat with Ollama;
- explicit RAG loading and retrieval services;
- embeddings and vector-store examples;
- LLM response evaluation;
- function calling;
- Spring AI MCP client and server configuration;
- standalone Java MCP client/server examples;
- Testcontainers-based integration tests;
- LangChain4j examples for comparison.

These examples confirm that the chosen Java ecosystem covers the required learning path. They are
study references, not a substitute for Nexo IA's architecture or tests.

## Deliberately excluded from the foundation

- Microservices and Kubernetes.
- Multiple Maven modules before boundaries are proven.
- Redis as a default dependency.
- LangChain4j alongside Spring AI without a measured reason.
- A vector extension before the RAG phase.
- Electron or Tauri before the web interaction is validated.
- Cloud authentication and multi-user tenancy.
- Spring Batch, Quartz, or an external workflow engine before scheduled-work requirements demand them.

## Environment snapshot

At the time of this decision, Java, Maven, Docker, and Ollama are not available on the current
environment's `PATH`. Node.js 23, npm 11, and Python 3.13 are available. The project will use the
Maven Wrapper, but installing a Java 25 runtime, Docker-compatible runtime, and Ollama remains setup
work before implementation and integration testing.

## Decisions still required

1. Confirm Maven rather than Gradle.
2. Confirm PostgreSQL for the real application from the first persistent milestone.
3. Decide whether the first interface is web-only or includes an early desktop shell.
4. Decide whether CSS Modules or a component library should define the initial design system.

## Initial version baseline

- Java 25 LTS, without preview features.
- Spring Boot 4.1.0 release line.
- Spring AI 2.0.0 release line.
- Apache Maven Wrapper pinned to Maven 3.9.16; Maven 4 remains pre-GA.

Before scaffolding, select the newest stable patch available within the accepted Spring Boot 4.1.x
and Spring AI 2.0.x lines and record any change in a pill and decision update.
