# Implementation status

## Current increment

**Increment 1 — local authentication foundation.**

The first implementation branch is `feat/project-scaffold`. It establishes the source layout and a
minimal vertical connection plus the first release `0.1` identity slice.

## Added

- Java 25 Spring Boot backend definition with the accepted Spring Boot and Spring AI BOM lines.
- Public, minimal `GET /api/v1/system` identity endpoint.
- Spring Security default-deny boundary with only system identity and health permitted anonymously.
- PostgreSQL, Flyway, JPA, and revocable token-session configuration.
- First foundation migration.
- React and TypeScript frontend shell that probes the backend system endpoint.
- Frontend behavior test and backend MVC security/contract test.
- Project-local `build-nexo` Skill containing the accepted code standards.
- Standard backend flow using a thin documented controller, service, separate response record,
  `ResponseEntity`, `BaseResponse`, and global personalized exception handling.
- Modular frontend system-status flow using a page, component, hook, TanStack Query, Axios, Zod,
  `styled-components`, typed theme, and centralized API error normalization.
- Private PostgreSQL base Compose service with no published database port.
- Idempotent development startup script that preserves named data volumes, recreates the Nexo
  containers, builds the backend image, and runs the frontend through Vite on Node.js 24 without a
  production frontend build.
- PostgreSQL 18.4 volume mounted at `/var/lib/postgresql`, matching the official image's versioned
  `PGDATA` layout introduced in PostgreSQL 18.
- Development-only Compose override that binds PostgreSQL exclusively to `127.0.0.1:5432` for a
  backend running outside the container network.
- Environment-variable contract for PostgreSQL, Ollama, server binding, and secure cookies.
- Atomic first-Owner bootstrap status and creation endpoints, protected by a database-level
  single-Owner constraint.
- Separate `user_account` and `password_credential` persistence with normalized identities and
  PBKDF2 adaptive password hashes.
- Username-or-email login, current-profile lookup, short-lived access JWTs in the `NEXO_ACCESS`
  `HttpOnly` cookie, and opaque refresh tokens in the narrower `NEXO_REFRESH` cookie.
- Server-side session and current-`jti` validation on every protected access, refresh-token hash
  storage, pessimistically locked rotation, replay detection, immediate compromise revocation, and
  logout cookie clearing.
- Active-session and recent-access endpoints with initial/last IP, user-agent, timestamps, expiry,
  outcome, and event type. Forwarded addresses remain untrusted until a deployment configures a
  trusted proxy boundary.
- SPA CSRF cookie/header flow and standardized JSON responses for unauthenticated and denied
  requests.
- REST and SSE chat requests share one refresh coordinator. If the short-lived access token expires
  before a stream starts, Nexo rotates the refresh token once and safely retries the message request;
  a second `401` opens the in-place re-authentication modal without printing the technical session
  error below a previous answer.
- React first-run Owner form, login form, authenticated profile state, and logout flow using Axios,
  TanStack Query, React Hook Form, and Zod.
- Password recovery and authenticated password change with reset-token expiry, password reuse
  protection, and revocation of other sessions.
- User-visible session and device management with current-session identification, remote revocation,
  ownership isolation, and revocation audit events.
- Persistent login-attempt history using hashed normalized identifiers and IP addresses, with
  configurable progressive lock periods after 5, 7, and 10 consecutive failures.
- Owner-only Member administration with creation, listing, activation, and disablement; disabling
  a Member immediately revokes every active session and records the administrative event.
- Self-service revocation of every other active session while preserving the current session.
- Owner-authorized inspection and individual revocation of Member sessions with target ownership
  validation and administrative audit events.
- Responsive application shell with a collapsible branded sidebar and mapped navigation for Chat,
  Projects, Cowork, tasks, Vaults, and Skills. The separate workspace header has been removed;
  account actions, profile photo, Settings, and Administration live in the sidebar account menu.
- Persistent light and dark visual themes derived from the Nexo IA color system, with system-theme
  preference as the initial default and an accessible control in Settings preferences.
- On compact screens the same sidebar becomes an accessible off-canvas panel opened by a floating
  menu control, preserving navigation and account actions without restoring a page header. Desktop
  navigation uses an emphasized active-item rail and a dedicated edge collapse control.
- Every routed workspace surface now owns the complete area beside the sidebar. The global shell no
  longer wraps route content in padding or a centered frame: Chat is edge-to-edge, while Home,
  Settings, Administration, and planned-capability states apply only their own responsive internal
  spacing. Outer page borders, rounded shells, and drop shadows no longer frame the central area.
- The authenticated shell now owns a bounded `100dvh` viewport with a complete `min-height: 0`
  layout chain. The document no longer scrolls behind the application: sidebar navigation, routed
  workspaces, Settings, Administration, Home, conversation messages, resource panels, and long
  feature content scroll inside their owning component. Shared workspace headers remain visible,
  compact navigation uses the same `56rem` breakpoint as the shell, and intentional calendar-grid
  horizontal scrolling remains inside the calendar surface.
- Projects, Knowledge Vaults, Skills, Cowork, and Tasks/calendar now have dedicated product
  workspaces instead of a shared placeholder. Tasks/calendar provides navigable month and agenda
  views, occurrence inspection, and session-only schedule drafts. Knowledge Vaults provides a
  searchable collection list, source explorer, governed scope creation, local file selection, and a
  bounded content preview for Markdown, text, JSON, and CSV files. A readable source can be attached
  explicitly to Chat; PDF and Office sources remain metadata-only until document ingestion exists.
  Skills provides a shared session catalog and complete editor for ownership, activation,
  instructions, output contracts, and declared dependencies. Typing `/` in Chat opens the enabled
  Skill catalog with keyboard navigation. The palette opens above the composer without being clipped
  by the conversation surface. The selected method is included explicitly in that message without
  granting its declared dependencies. Projects now owns a searchable project folder
  list, active-workspace detail, add/select flow, and an expandable view of the saved folder
  snapshot; Cowork exposes objective, milestone, activity, and decision surfaces.
- The Vault frontend now includes an interactive relationship map inspired by Obsidian. Vault and
  source nodes expose collection membership, bounded shared-term relationships, the current
  selection, and which readable sources are attached to Chat. The map is a visual client-side aid;
  it does not claim that authoritative ingestion, embeddings, relationship-aware retrieval, or
  automatic model navigation already exists.
- Session Vault drafts, attached-source selections, and personal Skill drafts are partitioned by the
  authenticated user identifier. Built-in Skills remain visible to every user, while a personal
  draft is returned only to its current owner. These preview catalogs are reset when the authenticated
  shell closes and non-authenticated query caches are removed on logout. Authoritative publication,
  sharing, and backend access control remain part of the governed Vault and Skill runtimes.
- The selected project workspace is shared through the project module's Zustand store and persisted
  in origin-scoped IndexedDB under the authenticated user's identifier. On supported desktop
  Chromium browsers, Projects opens the operating system's folder chooser through the File System
  Access API, stores the browser-managed read handle, detects Windows, Linux, or macOS for the UI,
  and restores the active workspace after reload without sending the handle or an absolute path to
  the backend. A user can switch it directly from the sidebar and see the same selection in Home,
  Chat, Projects, and Cowork.
- Selecting a local workspace records a bounded metadata snapshot of the project tree. Before Chat
  opens, Nexo revalidates browser permission and compares names, entry types, file sizes, and modified
  timestamps. Added, removed, or modified entries produce a prominent Chat warning with review and
  accept-current-structure actions. Generated or dependency-heavy directories such as `.git`,
  `node_modules`, `dist`, and `target` are not traversed, and no file content is copied into the
  snapshot. The same expandable snapshot is available from Chat's Project context panel. A folder
  captured for the first time is treated as the accepted baseline, so entering Chat does not report
  a change immediately; an existing or restored workspace is still checked before reuse.
- Workspace snapshots now inspect up to 20,000 entries, open the first directory level by default,
  provide path search and expand/collapse controls, and can be rescanned explicitly. Scan diagnostics
  distinguish ignored dependency/generated directories, depth or entry limits, and unreadable
  entries so a partial tree is never presented as complete.
- Preview records and newly created client drafts are labeled explicitly. Calendar drafts never
  execute, Vault files are not indexed, Skill drafts are not published, and Project/Cowork execution
  controls remain inactive until their authoritative backend and Companion APIs are implemented.
  Local Vault source content is read only for supported text formats and only in current memory; if
  the user attaches it to Chat, a bounded excerpt is deliberately sent inside each new message to the
  selected provider and displayed as message provenance. Detaching stops new inclusion; already sent
  excerpts remain in that conversation's history. The browser workspace bridge can enumerate
  metadata only after an explicit folder selection; it does not grant the model, backend, Cowork,
  commands, or editing tools access to workspace file contents. Persistent folder handles require
  Chrome or Edge on HTTPS or localhost. Firefox and Safari remain unsupported until their platform
  contracts or the native Companion provide an equivalent reusable directory capability.
- Settings now uses a responsive two-column workspace layout with sticky section navigation on
  desktop and horizontal overflow-safe navigation on compact screens. Existing profile, security,
  preferences, provider, and usage components remain the owners of their implemented behavior.
- The authentication surface now communicates the product promise through Understand, Build, and
  Stay in control pillars on desktop while preserving a focused, overflow-free form on mobile.
- Home, Chat, Projects, Cowork, Calendar, Vaults, Skills, Settings, and Administration are
  route-split with React lazy loading and the shared Nexo loading state so each business workspace
  loads only when opened.
- Brand color semantics are reflected in the interface: cyan identifies Nexo capabilities and
  processing, while coral highlights the authenticated person, decisions, notifications, planned
  states, and secondary navigation accents.
- The borderless, full-width Home surface is now the workspace entry point. It reads real provider,
  conversation, and seven-day usage state, links to recent conversations, exposes Chat as available,
  and presents Projects, Project Agent, Vaults, Skills, Cowork, and tasks/calendar against their
  documented future releases without implying that those runtimes already exist. Its primary copy
  frames Nexo as an objective-driven workspace that will analyze, plan, implement, validate, and
  report work under visible permissions and evidence. A request entered in the Home command surface
  is transferred through short-lived client state into the Chat composer without putting private
  prompt content in the URL or persistent browser storage. The command surface no longer presents an
  extra mode strip above the request field; future Plan, Build, and Cowork runtimes remain mapped in
  the capability area instead of appearing as inactive composer controls.
- Settings now has direct Profile, Security, Providers, and Usage navigation. Home provider and usage
  actions open their exact subsection, and Usage exposes a detailed, honest empty-state surface for
  token, request, latency, cost, provider, model, capability, and processing-location breakdowns.
- Providers now expose an authenticated Ollama status and installed-model discovery endpoint. Settings
  consumes it through TanStack Query and shows connected, unavailable, empty-model, and refresh states.
- The provider roadmap explicitly includes local Ollama, remote/home-server Ollama, OpenAI, Google
  Gemini, Anthropic API, and custom OpenAI-compatible servers. External credentials remain blocked
  behind the planned encrypted Secret Store; Nexo must never silently fall back to a remote provider.
- Provider Registry foundation is now persisted in PostgreSQL through a user-owned configuration
  table and authenticated CRUD API. Settings shows first-use setup for provider type, name, endpoint,
  and optional selected model, plus isolated provider listing and protected removal confirmation.
- Chat model discovery now resolves the requested provider configuration together with the
  authenticated user before any network access, applies `ProviderEndpointGuard`, and reads Ollama's
  real `/api/tags` catalog from the saved endpoint without holding a database transaction open.
  Available, empty, unavailable, disabled, and unsupported protocols are represented explicitly;
  protocols without an implemented adapter never receive fabricated model lists. The Chat picker
  groups every discovered model by provider and persists both provider-configuration ID and model on
  the conversation. A saved default remains fallback metadata rather than limiting the picker to one
  model. Remote vendor discovery, credential storage, and provider connection testing remain future
  increments.
- Browser navigation now uses React Router DOM with direct, refresh-safe workspace and Settings URLs.
  A shared confirmation modal built on the accessible Radix UI Dialog primitive protects logout,
  session revocation, and Member disablement before their irreversible effects are executed.
- Settings now groups Profile, Security and sessions, Providers, and Token usage. Profile photo and
  account identity are centralized there; security reuses the implemented password and device
  controls. Provider configuration and usage accounting continue to evolve behind their contracts.
- Settings includes persisted Preferences for the interface language, light or dark theme, and model
  Thinking. Thinking is off by default and applies to new requests; the preference is sent explicitly
  instead of relying on a provider default. Theme changes are applied immediately; the saved language
  choice is ready to drive the localization layer as translated interface content is introduced.
- Authenticated users can edit their own name, username, email, and date of birth from Profile;
  the frontend derives and displays age from that date instead of persisting a manually entered age.
  Username uniqueness is checked by the service and enforced case-insensitively by PostgreSQL;
  profile updates immediately refresh the authenticated frontend session data. Profile fields open
  in a protected read mode and require an explicit edit action. The sidebar account trigger and menu
  use the person's name and email.
- Conversations are private, ordered, renameable, and archivable. Message ordering is serialized by
  a pessimistic lock on the conversation row, so concurrent submissions queue instead of colliding
  with the unique sequence constraint.
- Each message records the release `0.1` execution contract: status, provider, model, input and
  output tokens, token source, latency, processing location, failure code, correlation identifier,
  and completion time. A partial unique index lets a conversation hold at most one non-terminal
  request, so a concurrent submission is rejected by the database rather than by an optimistic check.
- `ChatCompletionClient` is the provider boundary for streamed inference, with Ollama as the first
  adapter. It reads the newline-delimited JSON of `POST /api/chat` and takes `prompt_eval_count` and
  `eval_count` as provider-reported token counts. Ollama requests carry the explicit `think`
  preference, and its `message.thinking` stream is kept separate from final `message.content`. An
  unsupported provider type fails explicitly; Nexo IA never falls back to another provider.
- `ProviderEndpointGuard` resolves a registered endpoint before the server dereferences it and blocks
  a managed vendor type pointed at a loopback, link-local, or private address, while self-hosted
  Ollama and OpenAI-compatible servers may stay on a private network. It also reports whether
  processing is local or remote.
- Model requests walk the documented states — queued, streaming, cancelling, completed, cancelled,
  and failed. A cancelled generation keeps its partial answer under `CANCELLED` and is never promoted
  to `COMPLETED`; shutdown fails every in-flight request so an interrupted generation cannot reopen
  as if it had finished.
- Persistence runs in two short transactions with the stream between them, so no database connection
  or conversation lock is held while tokens arrive. Streaming runs on virtual threads.
- Conversation history is assembled within an explicit, configurable token budget that drops the
  oldest turns first, always sends the newest message, and excludes failed generations. Every model
  request begins with a budgeted `system` message defining the assistant as Nexo IA, treating the
  provider model only as its inference engine, following the user's language, and forbidding
  invented access or permissions. Provider reasoning is never persisted in conversation messages,
  so it cannot enter a later request's history even when its temporary display is enabled.
- `POST /conversations/{id}/messages/stream` streams typed `started`, `thinking`, `token`, `usage`,
  `completed`, `cancelled`, and `error` events over SSE, with a companion cancel endpoint. A
  `thinking` event is emitted only for a request that opted in. The request is reserved before the
  emitter opens, so a missing conversation, an unselected model, a busy conversation, or an invalid
  body still answers with the normal `BaseResponse` envelope and status. Authenticated initial
  requests remain the authorization boundary; Spring Security permits only the subsequent `ASYNC`
  and `ERROR` redispatches needed to finish the already-open SSE response, avoiding an access denial
  after response commitment.
- The SSE connection is no longer the owner of model execution. Navigating to another conversation
  or losing the response connection stops only delivery to that reader; the server continues the
  reserved request and persists its terminal result. The frontend keeps one stream snapshot per
  conversation, marks running threads in the conversation list, restores their progress when the
  user returns, and polls persisted active messages after a page-level reconnection. Explicit cancel
  remains authoritative, and logout requests cancellation before clearing session-only state.
- The chat interface streams answers through a dedicated fetch client with Zod-validated frames,
  exposes loading, empty, error, disconnected, streaming, cancelling, cancelled, and completed
  states, and reports model, token usage, latency, and processing location on completed answers. An
  unterminated final SSE frame is flushed when the transport closes, so a completed persisted answer
  is not lost at the browser boundary. The send and stop controls stay as compact right-aligned
  composer actions. While a request is active, a persistent spinner and elapsed timer remain at the
  end of the conversation, survive switching between chats, and distinguish preparing, generating,
  and stopping states. Completed answers label the measured response time. An estimated token count
  is always labelled. The composer derives a private recent-prompt history from the selected
  conversation's persisted user messages, removes duplicated prompts and internal context envelopes,
  and exposes both a compact up-arrow list and terminal-style `ArrowUp`/`ArrowDown` recall. Conversation
  creation uses an accessible dialog. The workspace now follows the Nexo visual semantics: cyan
  identifies the assistant and processing, coral identifies the person, model and privacy context
  stay visible in the conversation header, and Chat, Agent, tools, and image capabilities live in the
  composer without replacing the thread.
  Chat uses the wider application workspace, a compact one-line header, per-message copy actions,
  and Nexo-branded conversation loading. It does not present request setup or time-to-first-token as
  model Thinking. When enabled in Preferences, only real, explicitly classified provider reasoning
  is shown in a subtle live trace labelled as not saved; the trace is cleared at the terminal event
  and excluded from persistence and future context. When disabled, Nexo asks the provider not to
  generate it and discards any reasoning a model still emits. A minimized resource rail keeps the
  selected Project tree, attached Vault sources, governed implementation plan, Agent tasks,
  generated artifacts, and media oriented at the right edge and expands the selected section on
  demand. The expanded conversation list remains a scroll-owning left panel and becomes an overlay
  drawer on compact screens. When minimized it disappears completely, preserves the active
  conversation, and is reopened from an icon-only header control instead of consuming width as a
  rail. The Chat workspace establishes its own paint layer so internal drawers and counters cannot
  overlap the application's primary sidebar.
  Assistant and user content is rendered as safe GitHub-flavoured Markdown; headings, lists, tables,
  links, inline code, fenced code, and `diff` blocks have dedicated readable presentation, copy
  controls, and added/removed-line treatment. Selected Skills and Vault sources remain visible on
  the sent turn. Completed answers show input, output, total, context-budget usage, latency, and
  processing location; Chat also shows conversation and all-time account token totals. Model changes
  update optimistically, report persistence progress or errors, and roll back on failure instead of
  silently snapping to the previous selection.
  Media resources have a typed progress surface for runtime-reported percentage, elapsed time, and
  remaining-time estimates; an indeterminate state is used when the image runtime cannot provide a
  real percentage. The generation adapter itself remains pending the local image runtime.
  Capabilities without a connected runtime remain explicit previews or empty states instead of
  suggesting work was executed.
- A unified security audit trail is recorded in `audit_event` and inspectable only by an Owner at
  `GET /api/v1/admin/audit`, filterable by action and actor. It covers bootstrap, Member creation,
  disablement, restoration, and session revocation, provider creation, update, and removal,
  conversation creation, rename, archive, and model selection, and model requests started, completed,
  cancelled, and failed. Each model-request event carries the `correlation_id` that ties it to its
  message. The trail stores a short, safe detail only — never passwords, tokens, or message content —
  and session login and logout remain in the member-facing `access_event` feed. See D-022.
- Every Owner-only endpoint now answers `403` rather than `500` when a Member calls it: a
  method-security denial is handled explicitly by the global advice. See PILL-011.
- Personal usage accounting is now readable. `GET /api/v1/usage` reports the authenticated member's
  own request counts by terminal state, input and output tokens, average latency, provider-reported
  versus estimated counts, a per-day token series, and breakdowns by model and processing location,
  over a selected window. Aggregation reads from the recorded messages and is scoped to the caller in
  every query; the Settings usage surface renders it with a stacked per-day chart and honest empty
  states.
- One hundred and three passing default backend tests and one hundred and two passing frontend tests, including cross-user
  isolation for conversations and provider configurations, a deterministic Ollama protocol fake, and
  context-budget behaviour. The authentication flow was verified against a disposable PostgreSQL
  18.4 instance: migrations, bootstrap, login, authenticated profile, and logout. Every migration was
  reapplied to an empty PostgreSQL 18.4 database, and the active-request index was verified to reject
  a second concurrent request and to accept one again after the previous request became terminal.
- A Testcontainers test starts the complete application context against a disposable PostgreSQL 18.4
  instance and asserts that every migration applied and that the active-request index exists. It
  exists because unit tests construct their collaborators directly and therefore cannot prove that
  the container is able to supply them. It carries the `docker` tag: the container image builds
  itself inside a builder with no Docker daemon, so that build runs the remaining tests
  and the daemon-dependent ones run on a developer machine or a Docker-enabled CI job.
- An opt-in smoke test proves streaming, provider-reported token accounting, and cancellation against
  a real Ollama installation: `./mvnw test -Dexcluded.test.groups= -Dgroups=ollama`.

## Intentionally incomplete

- The Maven Wrapper 3.9.16 and multi-stage backend/frontend container images are present. Their
  complete container build and runtime verification remains a release check.
- The official Skill validator cannot run in the preserved environment because its `PyYAML`
  dependency is absent. Equivalent frontmatter, naming, key, and length checks pass through Node;
  rerun the official validator when the dependency is available.
- The base Compose service starts PostgreSQL, backend, and the production frontend image.
  Development uses `compose.dev.yaml`; production must not apply that database-port override.
- Organization membership beyond installation-level Owner/Member roles remains a subsequent
  identity increment.
- Personal usage is aggregated and shown. Organization-level summaries remain a subsequent increment
  because they require the organization entity, and pricing, budgets, and quotas stay out of scope.
- The organization-level audit *view* that unions session and domain trails is a later increment.
- Agent mode remains a visible choice without a runtime: it must expose its plan, tools, limits,
  approvals, evidence, and stop reason before it can be enabled. Image jobs remain pending the local
  ComfyUI runtime.

## Next verification

Next delivery checks:

1. build and run the existing multi-stage backend and frontend images;
2. verify the complete Compose runtime against PostgreSQL and Ollama.
