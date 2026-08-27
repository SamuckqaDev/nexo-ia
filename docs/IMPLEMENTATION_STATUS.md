# Implementation status

## Current increment

**Current verified foundation — multi-user Agent runtime with optional local Desktop workspace
inspection and governed file writes.**

The first implementation branch is `feat/project-scaffold`. It establishes the source layout and a
minimal vertical connection plus the first release `0.1` identity slice.

## Added

- Optional Electron `desktop/` runtime paired through a ten-minute single-use code and revocable
  device credential. It maintains an outbound authenticated WebSocket, keeps absolute paths and the
  raw credential encrypted on the device, and reports capability/heartbeat state without requiring
  an open browser tab. Compose keeps loopback as the default and exposes an explicit
  `NEXO_BIND_ADDRESS` opt-in for trusted cross-machine development; the production Nginx gateway now
  forwards the authenticated runtime WebSocket upgrade.
- Owner-isolated device inventory and opaque local Workspace bindings. Conversations persist the
  selected binding; Projects and Chat list its online, changed, missing, or error state and lazily
  browse the local tree through the authenticated runtime.
- The Spring AI Workspace callbacks now route transparently to either server storage or Nexo
  Desktop. The local implementation provides bounded reads and explicit-request file writes and
  enforces relative-path
  containment, sensitive/binary/oversize denial, ignored dependency/build trees, fixed Git argument
  arrays, task evidence, and server audit. Project files are not copied to the server. See D-033.

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
  Projects, Cowork, tasks, Teams, Vaults, and Skills. The separate workspace header has been removed;
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
- Project execution now uses owner-scoped server Workspace registrations. Managed directories live
  below a server-owned root; existing projects may be mounted only by relative path below an
  explicitly configured import root. The selected Workspace is persisted on each Conversation and
  restored across browsers. Projects, the Chat header, Agent Context, and the conversation Workspace
  panel consume the authenticated server catalog; the sidebar no longer maintains a competing
  browser-local active project. See D-032.
- The server computes a bounded, deterministic tree fingerprint and Git HEAD baseline. Added,
  removed, modified, or same-size timestamp changes produce a visible Chat warning; refresh accepts
  the new baseline without editing project files. Trees are loaded lazily in Projects and the Chat
  Workspace panel. Generated/dependency directories, symlinks, sensitive files, binaries, invalid
  UTF-8, oversized files, traversal, and absolute paths are rejected or omitted centrally.
- Workspace fingerprinting inspects at most 20,000 relevant sorted entries. The browser expands the
  server tree lazily with bounded pages and displays omissions and truncation instead of presenting
  a partial result as complete.
- Preview records and newly created client drafts are labeled explicitly. Calendar drafts never
  execute, Vault files are not indexed, Skill drafts are not published, and Project/Cowork execution
  controls remain inactive until their authoritative backend and Companion APIs are implemented.
  Local Vault source content is read only for supported text formats and only in current memory; if
  the user attaches it to Chat, a bounded excerpt is deliberately sent inside each new message to the
  selected provider and displayed as message provenance. Detaching stops new inclusion; already sent
  excerpts remain in that conversation's history. Workspace file reads are a separate server-side,
  permission-resolved Agent capability; arbitrary Project/Cowork commands remain unavailable.
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
  model. Remote vendor discovery and credential storage remain future increments.
- `POST /api/v1/providers/configurations/test` lets an authenticated user test a provider type and
  endpoint before saving it. It runs the same `ProviderEndpointGuard` check and typed
  available/empty/unavailable/unsupported catalog status as saved-provider discovery, persists
  nothing, and never leaks the tested endpoint in its error message. The Settings provider setup form
  now calls it through a "Test connection" action before saving, showing the resulting status inline.
- The conversation composer now creates a conversation automatically from the first typed message
  instead of requiring a separate "New conversation" dialog, deriving the initial title from that
  message. "New conversation" clears the active selection, draft model choice, and pending message
  state without a full-page reload, and works identically whether the account already has history or
  none. The chat header conversation title is editable inline from the header and from each sidebar
  item, saving through the authenticated rename endpoint. The context assembler's identity system
  message now includes the authenticated username resolved from the session user ID, never a
  browser-supplied value.
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
- Ollama model discovery records tool calling and Thinking as separate advertised capabilities. The
  model picker identifies both, and an explicitly unsupported Thinking request is suppressed instead
  of silently retrying an ordinary answer while the interface implies that reasoning is active.
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
  adapter. `SpringAiChatCompletionClient` builds a request-local `OllamaChatModel` and streams through
  Spring AI 2.0.1 `ChatClient`; `SpringAiOllamaEmbeddingClient` uses `OllamaEmbeddingModel`. Spring AI
  reads the provider protocol and reports usage/finish metadata, while Nexo preserves the explicit
  `think` preference and keeps `message.thinking` separate from final content. An unsupported provider
  fails explicitly; Nexo never falls back to another endpoint, model, or provider.
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
- `POST /conversations/{id}/messages/stream` streams typed `started`, `thinking`, `agent_state`,
  `tool_started`, `tool_completed`, `plan_updated`, `token`, `usage`, `completed`, `cancelled`, and
  `error` events over SSE, with a companion cancel endpoint. A
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
  composer actions. While a request is active, a minimal inline spinner and elapsed timer remain at
  the end of the conversation without a status card or explanatory block, survive switching between
  chats, and expose preparing, responding, and stopping states through accessible labels. Completed
  answers label the measured response time. An estimated token count
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
  demand. The Plan section renders the conversation's latest persisted plan revision, follows live
  `plan_updated` events during execution, and restores the correct plan after changing conversations
  or reloading instead of showing placeholder steps. The expanded conversation list remains a
  scroll-owning left panel and becomes an overlay drawer on compact screens. When minimized it
  disappears completely, preserves the active
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
  The Tasks resource has a typed progress surface for runtime-reported percentage, elapsed time, and
  remaining-time estimates; an indeterminate state is used when the image runtime cannot provide a
  real percentage. Parallel image jobs and Agent tool executions remain separate, inspectable task
  cards. Media is reserved for the gallery of completed conversation outputs.
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
- The backend Workspace module now owns registrations, managed/mounted bindings, access ceilings,
  live status, refresh, paged tree and bounded text preview under `/api/v1/workspaces`. Knowledge
  Vault references are preserved and deletion is rejected while a Vault still references the
  Workspace. The former frontend preview catalog is no longer authoritative for Chat or Projects.
- Local Postgres now runs `pgvector/pgvector:0.8.6-pg18-bookworm`. `KnowledgeVault`, `KnowledgeSource`,
  and `KnowledgeChunk` are persisted with owner/workspace-scoped authorization
  (`GET/POST/PUT/DELETE /api/v1/knowledge/vaults`, `GET/POST /api/v1/knowledge/vaults/{id}/sources`,
  `GET /api/v1/knowledge/sources/{id}/ingestion`, `DELETE /api/v1/knowledge/sources/{id}`). Scope
  `personal` and `workspace` are enforced for real; `project`, `team`, and `organization` are part of
  the contract but always rejected with `UnsupportedVaultScopeException` until their own backend
  entities exist.
- Uploading a source (Markdown, plain text, JSON, or CSV, capped at 3 MB) runs a bounded, synchronous,
  idempotent-by-content-hash pipeline: normalize, chunk (~1,200 characters, 200 overlap, capped at 300
  chunks), embed through a new `EmbeddingClient` provider boundary (Ollama only, mirroring
  `ChatCompletionClient`, resolved through the caller's own registered provider, never required for
  ordinary chat), and persist. A recognized-but-unextractable MIME type is stored `UNSUPPORTED`,
  metadata only; a pipeline failure is stored `FAILED` with a safe error code and correlation id,
  never a raw stack trace.
- `RetrievalService` ranks chunks through a hand-written JPQL query
  (`KnowledgeChunkRepository.findAuthorizedNearest`) that joins chunk → source → vault and filters by
  owner and archived/status *before* ranking by `cosine_distance` — an unauthorized or archived chunk
  cannot be ranked, let alone returned. Results below a minimum similarity, or an unavailable embedding
  provider, resolve to an explicit empty result; there is no lexical fallback in this release.
- Chat accepts a typed `knowledgeVaultIds` field (bounded to 8), replacing the vault portion of the
  former client-side `[NEXO_EXPLICIT_CONTEXT]` prompt prefix. Retrieval runs server-side before the
  model request; its citations are inserted as an explicit untrusted-context system message, persisted
  on the assistant message (never the retrieved excerpts themselves), and rendered as citation chips
  on completed answers. Retrieval failure never fails an ordinary chat request.
- The frontend gained a backend-facing Knowledge Vault API layer (Zod schemas, Axios functions,
  TanStack Query hooks for vaults, sources, and the minimal backend Workspace) and a workspace picker
  in `CreateVaultForm`. `VaultsPage`, the Chat Knowledge bar, and the conversation resources panel now
  consume that authenticated catalog; selecting a Vault in either Chat surface sends the same real
  backend UUID to server-side retrieval.
- The authenticated `GET /api/v1/knowledge/graph` endpoint projects the real pgvector index into a
  bounded semantic graph (Vault, source, and chunk nodes; containment and cosine-similarity edges).
  Owner filtering happens in the repository joins before graph assembly, vectors never reach the
  browser, and the movable/resizable/maximizable frontend Workbench adds search, zoom, chunk detail,
  document-only collapse, and indexed-excerpt inspection. See D-027.
- The authenticated MCP registry now persists Docker catalog and personal Streamable HTTP
  connections per user through migration V27. The backend reads Docker's live catalog with a
  reviewed free-first fallback, discovers real server tools through the official MCP Java SDK,
  preserves explicit selections, and rejects secrets, shared configuration, arbitrary STDIO, and
  private endpoints unless the operator deliberately opts into the last boundary.
- Agent mode attaches only the authenticated user's enabled MCP tools through Spring AI
  `SyncMcpToolCallback`. Request-owned clients close after inference; governance caps external calls,
  denies duplicate arguments, bounds results, honors cancellation, persists safe tool evidence, and
  writes correlated audits. The responsive MCP Hub separates Docker and personal servers, owns its
  internal scrolling, validates API payloads, and requires discover → select tools → enable. See
  D-030 and [MCP runtime and implementation plan](MCP_RUNTIME.md).
- MCP connection creation now flushes the persistence context before building the response, so
  Hibernate-managed `createdAt` and `updatedAt` values satisfy the frontend's strict API schema
  instead of occasionally arriving as `null`. Shared buttons use a smaller default footprint and
  MCP actions use the dedicated compact size, including connection, inspection, selection, and
  enablement actions.
- MCP tool snapshots now serialize the public tool identifier consistently as `externalName`, matching
  the strict frontend schema and preventing one discovered tool from breaking the owned-connection
  list. Contract failures render a controlled recovery message instead of raw Zod diagnostics, while
  catalog and inspector actions remain content-sized and single-line in the compact layout.
- Agent mode is usable directly from the chat composer. Its compact context inspector shows the
  selected backend Vaults, enabled owned MCP servers/tools, query failures, and whether the selected
  Ollama model advertises tool calling. Model discovery reads capability metadata and falls back to
  `/api/show`; explicitly incompatible models are labeled and cannot start a broken Agent run.
  Selected Vaults are described to the model as on-demand `search_knowledge` scope, while exact
  `mcp_*` names are identified as callable tools instead of generic context text.
- Runtime capability questions now come from the exact request callback snapshot rather than model
  recall. Explicit external research compacts away stale assistant refusals, requires a matching
  `mcp_*` call, buffers provider prose until tool evidence exists, and rejects a false successful
  answer when the selected model ignores the required tool.
- Knowledge-only Agent answers are released through a citation-grounding guard. Empty Vault results
  become a deterministic no-evidence response, and an HTTP link is accepted only when it appears
  verbatim in a retrieved excerpt; otherwise Nexo replaces the model prose with the exact bounded
  Vault citations instead of persisting an invented source.
- Knowledge source creation now flushes Hibernate timestamps before serializing both uploaded and
  Agent-authored sources, and the frontend accepts both source kinds. Source rows no longer archive
  knowledge when clicked; only a compact explicit Remove action followed by confirmation can do so.
- Explicit research now narrows the callable MCP set before inference: ordinary research exposes
  search/query/find tools, while a request containing a concrete URL exposes fetch/content/open
  tools. Failed or denied MCP evidence is reported as a failed lookup and can no longer authorize a
  model-authored answer.
- Every Agent request now receives a durable visible fallback plan before inference. A compliant
  model can replace it through `update_plan`; a deterministic decomposer also turns the actual user
  objective into bounded, verifiable steps with a concise title and observable description, so small
  models no longer receive the same generic three-item plan. The compact Plan surface shows numbered
  steps, descriptions, status, revision, and progress. A separate Tasks surface shows lifecycle,
  plan publication, and only persisted tool executions with timestamp, duration, terminal status,
  and safe citations. A normal response completes only fallback steps supported by evidence;
  Knowledge, memory, and MCP steps stay pending when their tools did not succeed. The capability envelope distinguishes
  exact executable tools from language abilities,
  explicitly reports when no MCP tool is connected, and points to the Hub. The Hub no longer uses a
  perpetual wait cursor for unavailable Docker actions and shows the container runtime boundary.
- The Compose development profile now starts pinned Docker MCP Gateway sidecars for Fetch and
  DuckDuckGo. They require bearer authentication, publish no host port, live on a dedicated network
  shared only with the backend, and are consumed through Spring AI's MCP SSE transport. The MCP Hub
  therefore presents those free catalog cards as executable instead of informational in local Docker
  development, while production keeps requiring an operator-owned gateway or Companion boundary.
- Migration V28 adds private personal memory with source-conversation/message provenance. Agent mode
  exposes the bounded Spring AI `remember` tool, whose callback receives owner and provenance only
  from server request scope. The 20 most recent memories owned by the authenticated user are framed
  as non-authoritative context in later Chat and Agent requests; exact duplicates are reused and a
  50-memory account cap prevents unbounded context growth. Authenticated APIs and the conversation
  workspace's Memory section provide inspection and deletion. See D-031.
- Migration V34 adds authenticated, per-user and per-conversation image-generation jobs. The local
  ComfyUI adapter queues the official workflow API, reads history and artifacts, persists binaries
  outside PostgreSQL, and records audit outcomes. Chat now has an honest Image mode; the Tasks rail
  shows every queued, generating, failed, or cancelled image job as a separate execution, including
  parallel jobs, while Media lists only completed images. Runtime discovery now exposes all installed checkpoints, the Image
  composer lets the user select one explicitly, and every queued job validates and records that
  selection. A one-shot Compose initializer repairs the media volume ownership before the
  unprivileged backend starts, including for existing development volumes. See
  [Local image generation](IMAGE_GENERATION.md).
- macOS, Linux, and Windows bootstrap scripts now install the development prerequisites, Ollama
  models, official ComfyUI checkout and local checkpoint before starting the Compose stack. Existing
  installations can still use the smaller `dev-up` scripts without reinstalling runtimes.
- Two hundred and thirty passing default backend tests and one hundred and twenty-six passing frontend tests,
  including cross-user isolation for conversations and provider configurations, a deterministic
  Ollama protocol fake, context-budget behaviour, and new Knowledge Vault isolation tests
  (`VaultServiceTest`, `RetrievalServiceTest`, `EmbeddingServiceTest`) proving an unsupported scope is
  rejected, a foreign workspace target is rejected, an unauthorized vault's chunks are never queried,
  results below the minimum score are empty, and an embedding-provider failure is never presented as
  an answer. The authentication flow was verified against a disposable PostgreSQL 18.4 instance:
  migrations, bootstrap, login, authenticated profile, and logout. Every migration through V21 was
  reapplied to an empty PostgreSQL 18.4 database, and the active-request index was verified to reject
  a second concurrent request and to accept one again after the previous request became terminal.
  A Testcontainers run also starts the complete Java 25 application context against PostgreSQL 18.6,
  applies all 34 Flyway migrations through the conversation-owned image job schema, and verifies the
  active-request index. The pgvector-backed local corpus remains migration-compatible.
- A Testcontainers test starts the complete application context against a disposable PostgreSQL 18.6
  instance and asserts that every migration applied, the Agent plan tool bean exists, and the
  active-request index exists. It
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
- Team/membership/profile governance now has a dedicated administration workspace. It lists the
  authenticated user's Teams, roles, profiles and members; administrators can add eligible users and
  create shared Team Vaults. Vault Explorer, Chat knowledge selection and the semantic graph expose
  Team ownership explicitly. Active-Team conversation selection, group usage quotas, shared
  media/artifacts, and content-matrix administration remain subsequent increments.
- Personal usage is aggregated and shown. Organization-level summaries remain a subsequent increment
  because they require the organization entity, and pricing, budgets, and quotas stay out of scope.
- The organization-level audit *view* that unions session and domain trails is a later increment.
- Agent mode has its first bounded runtime. Spring AI exposes up to ten request-owned callbacks
  directly through `ToolCallingAdvisor`; larger catalogs use `ToolSearchToolCallingAdvisor` and
  `toolSearchTool` for progressive discovery. Both paths cover `update_plan`, `remember`, conditional
  `search_knowledge`, and explicitly selected MCP callbacks, and provide
  `inspect_capabilities` for truthful runtime self-inspection. The index is recreated per execution
  from the authenticated user's authorized snapshot. Chat/Agent selection persists across Hub
  navigation, and thinking-only provider completions retry once without reasoning rather than being
  stored as empty successful answers. Plans and sanitized tool evidence persist on
  the assistant message, stream live, and
  restore after navigation or reload. Bounded server Workspace reads, single-file governed writes,
  and fixed read-only Git inspection are now available conditionally; terminal, browser, the complete
  approval Permission Engine, MCP secrets/configuration, resumable backend-restart
  execution, and multi-agent workers remain intentionally unavailable. Image cancellation,
  resumable ComfyUI jobs after backend restart, source-image editing, and remote image providers are
  later increments; local generation requires a running ComfyUI checkpoint.
- Knowledge Vault scopes `project`, `team`, and `organization` remain rejected by the ordinary Vault
  creation endpoint. Shared Team knowledge uses an authorized Team endpoint and persists a
  `PERSONAL` corpus with `owner_type=TEAM`; project/organization scope targets remain deferred.
- No lexical/full-text fallback exists for retrieval; below-threshold or provider-unavailable
  retrieval resolves to an explicit empty result, documented as a deferred follow-up in D-026.
- PDF and Office sources remain metadata-only (`UNSUPPORTED` status); only Markdown, plain text,
  JSON, and CSV are ingested and embedded.
- Explicit Markdown links, `[[wikilinks]]`, tags, frontmatter, and user-approved relationships are not
  parsed into graph edges yet. The current semantic links are inferred from chunk embeddings and are
  deliberately labeled separately from authored relationships.
- Agent mode conditionally attaches bounded Spring AI Workspace callbacks for the persisted
  conversation Workspace: file listing/read/search, Git status/diff, and project inspection. They
  reuse permission resolution, bounded calls, duplicate denial, cancellation, sanitized evidence,
  Tasks, and audit. Direct explicit write requests can additionally attach an atomic, SHA-protected
  `workspace_write_file` callback. Arbitrary commands, Git mutation, persistent approvals,
  multi-file transactions, artifacts, and worker delegation remain deferred. See D-032 and
  [Server workspaces](SERVER_WORKSPACES.md).
- No retry-without-reupload endpoint exists; retrying a failed source means re-selecting and
  re-uploading the same file.
- The Ollama embedding smoke test convention (`OllamaEmbeddingClient` against a real local Ollama with
  `nomic-embed-text` pulled) is documented but not yet written as an opt-in `ollama`-tagged test.

## Next verification

Next delivery checks:

1. verify the complete production frontend image;
2. pull `nomic-embed-text` in a local Ollama and manually smoke-test: create vault → add a Markdown
   source → wait for `READY` → open Chat → select the vault → ask a question → expand citations →
   confirm isolation with a second account;
3. validate the authenticated Knowledge Workbench visually at desktop and mobile breakpoints; the
   automated browser used for this change reached the login boundary but had no user session.
