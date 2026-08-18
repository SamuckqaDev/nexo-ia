# MVP and release strategy

## Objective

The first Nexo IA release proves one complete, secure product path rather than a collection of
partially connected AI features:

```text
Install Nexo IA
  -> bootstrap the Owner and default organization
  -> create a second member
  -> configure and verify local Ollama
  -> start isolated conversations
  -> stream and cancel model responses
  -> persist history
  -> attribute token usage and latency
  -> inspect the audit trail
```

The MVP is complete only when two users can use the same installation without accessing each other's
private conversations or execution metadata.

## Frozen MVP scope — release 0.1

### Installation and identity

- bootstrap exactly one Nexo Owner and one default organization on an uninitialized installation;
- authenticate with local username or email and password using revocable server-side sessions;
- allow the Owner to create, suspend, restore, and assign an Owner, Administrator, or Member role;
- expose current profile, organization membership, active sessions, logout, and logout-all;
- store adaptive password hashes only and apply login throttling without revealing account existence.

The first release creates users directly through administration. Email invitations, MFA, OIDC,
service accounts, multiple organizations per user, and self-registration remain later increments.

### Provider and model

- register one or more local Ollama endpoints owned by the organization;
- test endpoint health and list installed chat models;
- enable or disable models and preserve the selected model per conversation;
- require first-use provider setup when the authenticated user has no available provider;
- support user-scoped local, home-server, OpenAI, Gemini, Anthropic, and OpenAI-compatible endpoint
  configurations through the Provider Registry;
- test a connection before saving, refresh models from the provider, and isolate provider records by
  user; never silently fall back to another provider;
- never perform a silent remote fallback;
- record endpoint failure without exposing internal network or credential details to unauthorized
  users.

Remote providers, embeddings, image models, automatic routing, and provider credentials are outside
release 0.1. The contracts retain provider and processing-location fields so they can evolve without
rewriting conversations.

### Conversation

- create, list, open, rename, and archive a private conversation;
- send a message to the selected Ollama model and stream ordered events through SSE;
- cancel active generation and persist its terminal state honestly;
- persist user and assistant messages with ordering, model, provider, timestamps, status, and usage;
- assemble only the authorized conversation history within an explicit token budget;
- prevent concurrent requests from corrupting message order or conversation state.

Sharing, attachments, RAG, Memory, tools, Skills, Agent Mode, Cowork, and automations are outside the
first release.

### Usage, audit, and administration

- record input and output token counts reported by the provider or label a documented estimate;
- record model, provider, user, organization, conversation, request, latency, timestamps, status, and
  processing location;
- show personal usage to the member and organization summaries to authorized administrators;
- audit bootstrap, login success and failure, logout, user lifecycle, role change, provider change,
  conversation lifecycle, model request, cancellation, and administrative access;
- redact password material, session secrets, message content, and unnecessary personal data from
  operational logs and default audit views.

Budgets, billing prices, quotas, chargeback, anomaly detection, and audit export remain later work.

### Interface

- provide bootstrap, login, chat, conversation history, profile, provider/model administration, user
  administration, usage, and audit screens;
- expose loading, empty, error, disconnected, streaming, cancelling, cancelled, and completed states;
- support keyboard operation, visible focus, semantic labels, responsive layout, and the established
  English/Portuguese language model for product chrome;
- never hide a failed or cancelled request behind a successful assistant message.

## Explicit exclusions

Release 0.1 does not include:

- Knowledge Vaults, embeddings, `pgvector`, RAG, citations, or Memory;
- filesystem, commands, Project databases, Permission Engine effects, MCP, or Nexo Companion;
- plans, Agent Mode, Skills, Cowork, calendar, scheduled work, or autonomous execution;
- image generation, product/design Skills, pentesting, deployment automation, or mobile applications;
- remote model providers, public registration, federation, multi-organization tenancy, or internet
  exposure as a supported deployment.

These exclusions freeze implementation scope; they do not remove the corresponding roadmap items or
domain boundaries.

## Architecture slice

```text
React + TypeScript
  -> REST for commands and queries
  -> SSE for model-request events
Spring Boot modular monolith
  -> identity
  -> organization and access
  -> provider and model
  -> conversation
  -> usage
  -> audit
PostgreSQL
  -> Flyway migrations
Ollama
  -> local chat inference
```

The repository starts with one backend application and one frontend application. Java package
boundaries are enforced before separate Maven modules or services are considered.

## Initial domain records

| Area | Records |
|---|---|
| Identity | `user_account`, `credential`, `user_session`, `login_attempt` |
| Organization | `organization`, `organization_member`, `role_assignment` |
| Provider | `provider_configuration`, `model_definition`, `model_availability` |
| Conversation | `conversation`, `message`, `model_request`, `model_event` |
| Usage | `usage_record` |
| Audit | `audit_event` |

Every owned record carries a stable identifier, organization identifier where applicable, creation
and update timestamps, and optimistic version where concurrent modification matters. Database access
must apply ownership filters in application policy and repository queries; UI filtering is never a
security boundary.

## Required contracts

- versioned REST and SSE schemas with generated TypeScript types;
- one stable `BaseResponse` envelope containing HTTP-aligned code, safe message, and array data;
- explicit model-request states: `queued`, `streaming`, `cancelling`, `completed`, `cancelled`, and
  `failed`;
- idempotency or concurrency protection for message submission and cancellation;
- correlation identifiers across HTTP, model request, usage, and audit events;
- UTC persistence and explicit timezone conversion at interface boundaries.

## Security release gate

- passwords use a reviewed adaptive hashing configuration and never appear in logs or events;
- session cookies use appropriate `HttpOnly`, `SameSite`, and `Secure` behavior for the deployment;
- CSRF, authorization, login throttling, session revocation, and safe error responses are tested;
- cross-user and cross-role tests attempt direct identifier access, listing, update, archive, SSE
  subscription, usage access, and audit access;
- prompt and response content is not copied into audit events or structured operational logs;
- Ollama endpoints are restricted to authorized administrators and server-side requests;
- dependency, secret, and configuration scanning produce no unresolved release-blocking finding.

## Quality and operational release gate

- focused domain tests, repository integration tests against PostgreSQL, Ollama protocol tests with a
  deterministic fake, and critical Playwright journeys pass;
- one tagged real-Ollama smoke test proves streaming and cancellation without becoming a default CI
  dependency;
- Flyway migrations apply to an empty database and upgrade from the previous release baseline;
- health checks distinguish application, database, and Ollama availability without leaking secrets;
- shutdown does not leave a model request falsely marked as completed;
- the README documents prerequisites, configuration, startup, test, backup, and troubleshooting;
- the release has a reproducible demonstration and an evidence-backed acceptance report.

## Acceptance journeys

1. A fresh installation creates the Owner once and rejects a second bootstrap.
2. The Owner creates a Member; both can authenticate and revoke their sessions.
3. The Owner configures Ollama and enables an installed model.
4. Each user creates a conversation, receives streamed output, cancels one request, and reopens
   persisted history after restart.
5. Attempts to access the other user's conversation, SSE stream, usage detail, or messages fail
   without revealing whether the resource exists.
6. Personal usage is visible to its owner; organization usage and audit require the appropriate role.
7. Every security-relevant action is correlated in audit without password, session, or message-body
   leakage.

## Release sequence after 0.1

| Release | Outcome |
|---|---|
| 0.1 Foundation | Multi-user identity, local Ollama chat, persistent conversations, usage, and audit. |
| 0.2 Knowledge | Knowledge Vault Explorer, ingestion, embeddings, retrieval, citations, and scoped Memory. |
| 0.3 Governed tools | Workspaces, read/edit tools, Permission Engine, Privacy Gateway, and Project database safety. |
| 0.4 Agent platform | Plans, Goals, bounded Agent Mode, Skills, context governance, MCP Hub, and evaluation. |
| 0.5 Devices | Nexo Companion, pairing, inventory, notifications, and cross-platform computer control. |
| 0.6 Team work | Cowork, shared approvals, automations, calendar, budgets, quotas, and autonomy levels. |
| 0.7 Creative and assurance | Images, product/UX workflows, documentation engineering, authorized security validation, and operations. |

Each release is independently deployable and inherits every prior isolation, privacy, audit,
cancellation, recovery, and documentation gate.

## Change control

Release 0.1 scope is frozen after acceptance of this document. A proposed addition must identify the
user problem, why a later release cannot contain it, new risks, migration impact, tests, and the item
it replaces. Otherwise it enters the backlog for the appropriate later release.
