# Nexo IA — Claude Code Implementation Plan

## Mission

Bring the current `feat/project-scaffold` branch to a reliable local Chat 0.1 milestone. The
existing repository already contains authentication, provider registry, Ollama discovery,
conversation persistence, SSE streaming, cancellation, usage, audit, workspaces, Vault previews,
Skills previews, and the main product shell. Do not rebuild those areas from scratch. Inspect the
current implementation first, preserve working behavior, and implement only the missing or broken
parts required by the acceptance criteria below.

The main user journey is:

```text
login -> configure Ollama -> discover models -> create conversation -> select model
-> send message -> receive SSE tokens -> cancel or complete -> reopen persisted history
```

## Non-negotiable project standards

### Backend

- Java 25, Spring Boot 4.1, Spring AI 2.0, PostgreSQL, Flyway, JPA, Spring Security.
- Keep the modular-monolith business boundaries under `com.nexoia`.
- Controllers are thin: mapping, authentication principal extraction, validation, documentation,
  service delegation, and `ResponseEntity` only. No business rules in controllers.
- Business rules belong in services.
- Every DTO/request/response is a separate top-level file. Use records where appropriate. Never put
  DTO records inside services, controllers, entities, or provider clients.
- Use Lombok where it removes mechanical boilerplate. Do not generate unnecessary `Impl` classes or
  interfaces solely to satisfy a pattern.
- Use the existing `BaseResponse` envelope: success data is an array; no data is `[]`; errors have
  `data: null`.
- All application failures must be personalized `ApplicationException` subclasses handled by the
  existing global advice. Do not leak provider URLs, credentials, stack traces, or internal network
  information to clients.
- Do not add `try/catch` around ordinary business code. Catch only at infrastructure boundaries
  where a library/transport exception must be translated to a project exception. Never silently
  swallow an exception.
- Repository queries must enforce authenticated ownership. A user must never infer another user's
  conversation, message, provider, model request, usage, or audit data.
- Never put credentials, JWTs, cookies, passwords, prompts, or model responses in logs.
- Preserve the existing SSE state machine and cancellation semantics. Do not hold a DB transaction
  or row lock while model tokens are streaming.

### Frontend

- React 19, TypeScript strict mode, Vite, Axios, TanStack Query, React Hook Form, Zod,
  styled-components, Radix Dialog, Phosphor Icons.
- Feature modules live under `frontend/src/modules`; shared UI belongs under
  `frontend/src/shared/components`.
- Each reusable component has separate `index.tsx` and `styles.ts` files.
- Import named components directly; do not use namespace-style `S.Panel`/`S.Copy` imports.
- Keep all values typed. Do not use `any` or untyped event handlers.
- Use Axios through the shared client. Authentication is cookie-based; do not add auth tokens to
  request headers or browser storage.
- Use React Hook Form + Zod for forms.
- Prefer Promise chains (`then`/`catch`) for request flows; do not introduce `try/catch` in UI code.
- Every async surface must expose loading, empty, error, disconnected, streaming, cancelling,
  cancelled, and completed states where applicable.
- Use the Nexo theme, palette, typography, reusable `Loading`, snackbar, confirmation modal, and
  session revalidation modal. Do not create generic unbranded spinners.

## Phase 0 — Baseline and inventory

1. Read `docs/IMPLEMENTATION_STATUS.md`, `docs/MVP_AND_RELEASE_STRATEGY.md`, `docs/FEATURES.md`,
   `docs/TECH_STACK.md`, `pills/README.md`, and the project skill instructions.
2. Inspect the current conversation, inference, provider, auth, usage, audit, and frontend chat
   modules before editing.
3. Confirm the working tree and branch. Never modify `main`.
4. Run the existing focused backend and frontend tests before changes. Record failures with the
   exact command and classify them as environment, integration, or code failures.

## Phase 1 — Local runtime and provider readiness

1. Verify PostgreSQL migrations through `V16`, backend health, frontend Vite proxy, and Ollama
   reachability from the backend container.
2. Verify the configured Ollama endpoint is treated as a base URL and that `/api/tags` is called
   only after `ProviderEndpointGuard` approves it.
3. Verify the authenticated model catalog is scoped to the saved provider configuration and current
   user. Never use a global/default provider when the conversation selected another configuration.
4. Implement or repair a typed provider connection test before saving a configuration. The test
   must return a safe status and discovered models, must not persist secrets, and must use a
   personalized exception for transport/protocol failures.
5. Preserve explicit unsupported-provider behavior. Do not fabricate model lists or silently fall
   back to Ollama for OpenAI, Gemini, Anthropic, or other providers.
6. Keep remote credentials out of this milestone unless the existing Secret Store contract is
   already present and reviewed. If credentials are required, expose a clear blocked state and
   document the next increment.

## Phase 2 — Chat backend acceptance

Verify and repair the following without changing the established contracts:

1. Conversation CRUD is authenticated, user-scoped, renameable, and archive-safe.
2. Message submission validates the request, selected provider configuration, selected model, and
   conversation ownership before reserving work.
3. The reservation rejects a second active request for the same conversation with the personalized
   busy exception.
4. The context assembler includes only authorized, successful history within the configured token
   budget, always includes the newest user message, and excludes failed/cancelled generations.
5. Ollama `POST /api/chat` NDJSON is translated into typed `started`, `thinking`, `token`, `usage`,
   `completed`, `cancelled`, and `error` SSE events. Keep provider reasoning separate from persisted
   assistant content.
6. A disconnected SSE reader must not falsely complete or cancel the server-side model request.
   Explicit cancellation must persist partial output with `CANCELLED` status.
7. Shutdown must fail in-flight requests instead of leaving them apparently active or completed.
8. Persist usage, latency, processing location, provider, model, correlation ID, status, and failure
   code without storing secrets or duplicate assistant messages.
9. Verify the cancel endpoint cannot cancel another user's request or a terminal request.
10. Add or repair focused unit/controller/integration tests for ownership, ordering, cancellation,
    provider protocol parsing, endpoint guarding, token accounting, and error envelopes.

## Phase 3 — Chat frontend acceptance

1. Verify route loading and direct refresh-safe navigation to Chat.
2. Verify conversation list loading, empty, error, rename, archive, selection, and active-request
   indicators.
3. Verify model picker loading, provider grouping, disabled/unavailable states, persisted model
   selection, optimistic update rollback, and no fabricated remote models.
4. Verify the composer sends only the intended request fields, keeps selected Vault/Project/Skill
   context explicit, and does not place private prompt content in URLs or persistent storage.
5. Verify SSE parsing handles normal frames, typed events, malformed frames, reconnects, final
   unterminated frames, network failures, cancellation, and a second `401` session revalidation.
6. Verify the Nexo `Loading` component, inline response timer, thinking trace preference, snackbar,
   confirmation modal, and session-expired modal appear in the correct states.
7. Verify Markdown rendering is safe and readable for headings, lists, links, tables, inline code,
   fenced code, and diff blocks. Copy actions must not copy hidden metadata.
8. Verify switching conversations during streaming preserves each conversation's stream snapshot
   and restores persisted active messages after reconnection.
9. Add/repair tests for ChatPage, composer, message list, model picker, stream parser, stream hook,
   cancellation, and session expiry.

## Phase 4 — Security, observability, and failure review

1. Run cross-user tests against conversation IDs, message IDs, provider IDs, model requests, SSE,
   usage, and audit endpoints.
2. Confirm `401` and `403` are distinct and represented by the global response contract.
3. Confirm a stale access cookie triggers one coordinated refresh attempt, then opens revalidation
   without allowing interaction with the authenticated workspace.
4. Confirm frontend logs use `[NEXO-FRONT]` and backend logs use `[NEXO-BACK]`, while never logging
   passwords, tokens, cookies, prompt bodies, response bodies, or provider credentials.
5. Confirm provider errors are safe for clients but diagnostically useful in backend logs.
6. Confirm audit events contain safe metadata and correlation IDs, not message content or secrets.

## Phase 5 — Verification and documentation

Run all applicable checks:

```bash
git diff --check
./mvnw verify
npm test -- --run
npm run build
node scripts/build-docs-data.mjs
```

For the container path, use the existing script and preserve named volumes:

```bash
./scripts/dev-up.sh
```

Run the real Ollama smoke test only when Ollama is available:

```bash
./mvnw test -Dexcluded.test.groups= -Dgroups=ollama
```

Update `docs/IMPLEMENTATION_STATUS.md` so it accurately reflects the current Chat 0.1 state. Do
not claim that RAG, Agent Mode, Cowork execution, image generation, remote credentials, MCP, or
filesystem control are implemented when they are only preview surfaces or contracts.

## Definition of done

- The complete local Ollama chat journey works after a clean container startup.
- Two authenticated users cannot access one another's provider, conversation, messages, stream,
  usage, or audit records.
- Streaming, cancellation, reconnect, refresh, and session revalidation behave honestly.
- Backend and frontend tests pass, the frontend builds, migrations apply to a clean database, and
  `git diff --check` is clean.
- Documentation and implementation status match the actual code.
- Changes are committed in small semantic commits on `feat/project-scaffold`; never commit directly
  to `main` and never commit `.env`, tokens, cookies, or generated secrets.

## Claude Code execution protocol

Before each phase, report the files inspected and the hypothesis. After each phase, report changed
files, tests run, failures, and remaining risks. Stop and ask for direction if a change would need
new credentials, a destructive migration, a new external service, or a scope expansion beyond this
plan.
