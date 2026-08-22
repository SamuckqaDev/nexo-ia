# Nexo IA — Claude Code implementation plan: Spring AI 2.0 orchestration

## Mission

Replace Nexo IA's hand-written Ollama chat and embedding protocols with a real Spring AI 2.0
integration, make identity and runtime capabilities explicit in every model request, deliver
authorized Knowledge Vault context through Spring AI orchestration, and establish the first
governed tool-calling/agent loop without weakening the product's existing user isolation,
streaming, cancellation, persistence, citations, usage accounting, or audit guarantees.

This is an implementation plan for Claude Code. Work only in the current branch and workspace.
Read `skills/build-nexo/SKILL.md` and every reference it links before editing. Do not edit, stage,
delete, or commit the unrelated untracked `.claude/` directory. Do not create a commit or push;
Codex will review, validate, document the final result, and create the delivery commit.

## Current verified reality

- Backend baseline: Java 25, Spring Boot 4.1.0, Spring AI 2.0.0.
- `spring-ai-starter-model-ollama` is already declared in `backend/pom.xml`.
- Production code currently has no `org.springframework.ai` imports.
- `OllamaChatCompletionClient` manually posts to `/api/chat` and parses NDJSON.
- `OllamaEmbeddingClient` manually posts to `/api/embed`.
- `ConversationContextAssembler` hardcodes the Nexo identity in a Java text block.
- The authenticated RAG path already owns real PostgreSQL/pgvector data, filters Vault access by
  owner before ranking, and persists citations on assistant messages.
- Chat sends temporary `knowledgeVaultIds` from React component state. That selection is not durable
  per conversation and disappears after a page reload.
- A message with no selected/retrieved Vault receives no explicit runtime capability declaration;
  smaller models can therefore claim they searched knowledge even when `citations` is empty.
- The current custom provider boundary is intentional: each authenticated user registers an Ollama
  endpoint and model. A single application-wide model bean cannot replace this requirement.
- SSE started/thinking/token/usage/completed/cancelled/error events, explicit cancellation, terminal
  message persistence, usage totals, and background completion after browser disconnect already
  exist and are compatibility requirements.

## Delivery scope

This increment must deliver all of the following:

1. Spring AI-backed Ollama chat streaming through `ChatClient`/`OllamaChatModel`.
2. Spring AI-backed Ollama embeddings through `OllamaEmbeddingModel`.
3. Markdown-backed Nexo identity/rules loaded by the backend, not copied into Java.
4. A typed, per-request capability/context envelope that states what the model actually received.
5. Spring AI Advisor-based Knowledge Vault context injection with citations and explicit no-source
   state.
6. Durable, authorized Knowledge Vault selection per conversation.
7. A first read-only `search_knowledge` Spring AI tool and governed tool-calling loop.
8. Typed tool lifecycle events and persisted tool evidence sufficient for the chat to show what the
   agent actually did.
9. Compatibility tests proving no regression in streaming, cancellation, token accounting,
   isolation, RAG citations, and providers/models that do not support thinking or tool calling.
10. Updated decisions, implementation status, architecture documentation, and generated docs data.

Do not implement filesystem mutation, terminal execution, Git writes, browser automation, email,
external MCP tools, autonomous multi-agent workers, or destructive actions in this increment. The
agent foundation is real but read-only: its only executable capability is authorized knowledge
search. Later tools must reuse the same registry, permission, event, and audit contracts.

## Target request flow

```text
authenticated request
  -> reserve conversation messages
  -> resolve durable conversation Vault selection
  -> authorize user/provider/model/Vaults
  -> build CapabilityEnvelope
  -> build Spring AI messages from bounded conversation history
  -> Spring AI advisor chain
       1. identity/rules advisor
       2. capability envelope advisor
       3. deterministic selected-Vault retrieval advisor
       4. governed tool-calling advisor (read-only search_knowledge only)
       5. observation/audit hooks
  -> dynamic per-user OllamaChatModel
  -> ChatClient streaming response
  -> typed thinking/token/tool/usage/completed events
  -> persist terminal answer, citations, tool evidence, usage, and audit
```

Spring AI owns prompt/message/model/tool orchestration. Nexo IA continues to own authentication,
authorization, provider registry, context budgets, persistence, permission decisions, SSE contracts,
audit, and product state.

## Non-negotiable invariants

### Security and ownership

- Resolve every provider, conversation, Vault, source, chunk, tool, and tool argument for the current
  authenticated principal.
- Never trust model-provided owner ids, user ids, Vault ids, workspace ids, permissions, endpoints,
  or tool scopes.
- The model may request a tool; only deterministic Nexo code decides whether it is available and may
  execute.
- `search_knowledge` may search only the Vault ids already authorized and attached to the active
  conversation. Its model-facing schema must not contain `ownerId`.
- Never serialize raw embeddings, absolute paths, secrets, cookies, tokens, provider credentials, or
  unrestricted source documents into prompts, tool results, events, logs, or API responses.
- Retrieved documents and tool results are untrusted reference material and cannot redefine
  identity, capabilities, permissions, or system rules.

### Provider behavior

- Preserve per-user dynamic provider endpoints. Do not introduce a single global Ollama base URL as
  the authoritative chat path.
- Continue to run every endpoint through the existing endpoint normalizer/guard before constructing
  Spring AI clients.
- Do not silently fall back to another endpoint, provider, model, or remote service.
- A model without thinking support must still answer normally when thinking is disabled or when the
  existing controlled retry-without-thinking rule applies.
- A model without tool-calling support must remain usable in Ask mode through deterministic RAG. It
  must not receive fake tool results or silently pretend a tool ran.

### Streaming and persistence

- Keep the current browser SSE contract compatible; only add typed events.
- Browser disconnect must stop writes to that browser, not cancel model execution.
- Explicit cancel must dispose the Spring AI/Reactor subscription, preserve partial visible answer,
  persist `CANCELLED`, and emit the existing cancellation event when transport remains connected.
- Persist terminal state even if the browser navigated away.
- Do not keep a JPA transaction or connection open during model streaming or tool execution.
- Never persist private chain-of-thought. Thinking deltas may be transiently displayed only when the
  user's preference enables them; they never become ordinary conversation history.

### Framework use

- Use official Spring AI 2.0 contracts directly where they own the behavior:
  `OllamaApi`, `OllamaChatModel`, `OllamaEmbeddingModel`, `ChatClient`, `Prompt`, Spring AI messages,
  Advisors, `ToolCallback`, `ToolCallingManager`, and observability.
- Keep a project-owned provider-neutral boundary only because multiple provider types and dynamic
  user endpoints are real requirements. Do not create wrappers that only rename a Spring AI method.
- Do not retain the manual `/api/chat` NDJSON parser or manual `/api/embed` request/response DTOs as a
  hidden fallback.
- Do not replace the authorized pgvector repository query with a generic `VectorStore` query that
  cannot enforce the existing owner/Vault join before ranking.

## Phase 0 — Baseline and characterization

Before editing:

1. Read the mandatory Nexo Skill and all references.
2. Inspect the complete provider, inference, embedding, retrieval, conversation, SSE, usage, audit,
   security, and frontend chat flows.
3. Run focused existing tests for `OllamaChatCompletionClient`, `ModelRequestService`,
   `ConversationContextAssembler`, `EmbeddingService`, and `RetrievalService`.
4. Record the current event order, cancellation semantics, usage fields, context budget, citations,
   and persistence behavior in tests before replacing implementation.
5. Inspect Spring AI 2.0 source/API from the resolved Maven artifacts. Do not implement from 1.x
   examples or snapshot-only contracts.

## Phase 1 — Prompt resources and typed context envelope

### Resources

Create versioned backend resources:

```text
backend/src/main/resources/prompts/
  nexo-identity.md
  nexo-rules.md
  knowledge-context.md
  capability-envelope.md
```

- Move the existing identity text out of `ConversationContextAssembler` into the resources.
- Identity must say the assistant is Nexo IA and the provider model is only its inference engine.
- Rules must require the user's language, truthful capability reporting, source attribution, and no
  invented access or execution.
- Knowledge context must label excerpts as untrusted references.
- Capability envelope must distinguish available, selected, retrieved, unavailable, and unsupported.
- Load resources once through a dedicated concrete service and fail application startup with a safe,
  specific error when a mandatory resource is missing/blank.
- Tests must prove editing the resource changes the system prompt without editing Java.

### Context contract

Create explicit domain records, not a map of arbitrary strings:

- `ModelContextEnvelope`
- `CapabilityManifest`
- `KnowledgeCapability`
- `WorkspaceCapability`
- `SkillCapability`
- `ToolCapability`
- `ContextSourceSummary`

At minimum the model-facing envelope states:

- authenticated username;
- active conversation mode;
- selected provider/model and processing location;
- active workspace presence/name and whether server-side access exists;
- selected Vault names/ids count;
- retrieved source/chunk count;
- active Skill names;
- exact tools exposed for this request;
- explicit constraints such as `knowledgeSearchPerformed=false` and `sourcesRetrieved=0`.

Render the envelope deterministically as a system message. Keep internal ids out unless the model
needs an opaque id for an allowed tool; prefer safe display names.

## Phase 2 — Dynamic Spring AI model factory

Create one business-owned factory responsible for constructing Spring AI provider objects from an
already authorized provider configuration:

```text
provider/springai/
  SpringAiModelFactory.java
  SpringAiChatCompletionClient.java
  SpringAiEmbeddingClient.java
  SpringAiMessageMapper.java
```

Names may change to fit the module, but responsibilities must remain focused.

For Ollama:

- construct `OllamaApi` with the authorized normalized endpoint;
- construct `OllamaChatOptions` with the conversation's selected model and thinking option;
- construct `OllamaChatModel` with project observation registry/retry policy;
- construct `ChatClient` for the request/model;
- construct `OllamaEmbeddingModel` with the configured embedding model;
- never mutate a singleton model's endpoint or model options across users;
- avoid an unbounded cache. If model clients are cached, key by normalized endpoint plus non-secret
  configuration and use a strict maximum/expiry; a request-scoped construction is acceptable first.

Add focused tests proving two users/endpoints cannot reuse each other's model configuration.

## Phase 3 — Migrate chat streaming to Spring AI

Replace `OllamaChatCompletionClient`'s protocol implementation with a Spring AI-backed adapter:

1. Map Nexo's bounded provider-neutral messages to `SystemMessage`, `UserMessage`, and
   `AssistantMessage`.
2. Build a Spring AI `Prompt`/`ChatClient` request with the runtime options.
3. Stream through `ChatClient.stream().chatResponse()` or the most suitable stable Spring AI 2.0
   streaming contract.
4. Extract answer deltas from `AssistantMessage.getText()`.
5. Extract Ollama thinking deltas from Spring AI response metadata/property `thinking`; never append
   them to answer content.
6. Capture provider prompt/completion usage from `ChatResponseMetadata.getUsage()` and preserve the
   existing `TokenSource.PROVIDER` semantics.
7. Capture finish reason through Spring AI generation metadata.
8. Wire explicit Nexo cancellation to Reactor disposal/cancellation and return the partial answer.
9. Translate Spring AI/provider transport failures into the existing controlled provider exception;
   never expose a third-party stack trace/message to the API.
10. Keep retry behavior bounded. Do not retry a cancelled request, authorization failure, invalid
    endpoint, or arbitrary model error as if it were only unsupported thinking.

Delete the now-unused manual protocol DTOs and parsing code:

- `OllamaChatRequest`
- `OllamaChatStreamChunk`
- `OllamaChatStreamMessage`
- direct NDJSON reader logic

Update or replace protocol tests with Spring AI adapter contract tests backed by a deterministic fake
Ollama HTTP server. The fake must still prove the actual `/api/chat` request, streaming deltas,
thinking, usage, failure, and cancellation through Spring AI.

## Phase 4 — Migrate embeddings to Spring AI

Replace the manual embedding HTTP adapter with `OllamaEmbeddingModel`:

- build `OllamaEmbeddingOptions` using the configured model;
- call a batch `EmbeddingRequest` so input order remains stable;
- validate non-empty result count, dimensions, and one output per input;
- map results into the existing provider-neutral `EmbeddingOutcome`;
- preserve `EmbeddingProviderUnavailableException` as the controlled business failure;
- preserve per-user provider resolution and endpoint isolation;
- retain the deterministic test embedding client;
- delete `OllamaEmbedRequest`, `OllamaEmbedResponse`, and direct `RestClient` embedding calls.

Ingestion and retrieval tests must prove that vectors persisted and queries ranked before this change
still work through the Spring AI adapter.

## Phase 5 — Spring AI Knowledge Advisor and durable Vault selection

### Durable selection

Replace temporary React-only Vault selection with server-owned conversation state.

- Add a Flyway-managed relation such as `conversation_knowledge_vault` with conversation/Vault foreign
  keys and a unique composite key.
- Add a service operation and authenticated endpoint to replace the selected set atomically.
- Authorize both conversation ownership and every Vault before storing the relation.
- Return the selected Vault ids in the conversation response contract.
- On frontend toggle, persist through TanStack Query and render pending/error state.
- Changing chats must restore that chat's selection. Reloading the page must not clear it.
- Sending a message resolves the authoritative selected set from the backend; client ids are never
  the authorization source. Remove or deprecate the transient request field safely after frontend
  migration.

### Advisor

Implement a Spring AI `CallAdvisor`/`StreamAdvisor` (one Advisor type implementing the supported
stable contracts when possible) that receives an already-authorized retrieval result and:

- injects bounded citations through the `knowledge-context.md` template;
- injects an explicit no-retrieval/no-results statement when appropriate;
- records retrieval metadata in Advisor context for observations/tests;
- never performs repository access with model-provided ids;
- never exposes vectors or full source bodies;
- preserves citation order, score, Vault/source labels, and chunk ordinal.

Keep the current authorized repository-first pgvector retrieval. Spring AI owns orchestration and
message augmentation; Nexo owns the security-sensitive vector query.

The final answer must carry exactly the citations that were delivered to the model. If zero citations
were delivered, the UI must say `No Vault sources used` and the capability envelope must prohibit the
model from claiming it searched or found internal knowledge.

## Phase 6 — First governed Spring AI tool and agent loop

### Read-only knowledge tool

Create a Spring AI `ToolCallback` named `search_knowledge` with a narrow input record:

```text
query: non-blank bounded string
limit: optional bounded integer
```

Do not accept owner id, endpoint, arbitrary Vault ids, SQL, filters, or paths from the model. The tool
closure/context is built from the authenticated request and resolves only the active conversation's
authorized Vault set.

The structured result contains:

- `status`: `FOUND`, `NO_RESULTS`, `UNAVAILABLE`, or `DENIED`;
- bounded citation objects;
- safe Vault/source labels and chunk ordinal;
- score and bounded excerpt;
- an explicit statement that no source was found when empty.

### Governed loop

Use Spring AI 2.0's `ToolCallingAdvisor`/`ToolCallingManager`, but keep execution controlled by Nexo:

- register only tools enabled for the current mode and permissions;
- execute through a Nexo tool registry/policy gate, not direct arbitrary bean discovery;
- cap tool rounds and total tool calls per request;
- cap tool result bytes/tokens;
- stop on cancellation, denial, repeated identical calls, budget exhaustion, or provider failure;
- persist tool name, sanitized arguments digest/summary, status, duration, citation/evidence metadata,
  and correlation id;
- audit started/completed/denied/failed tool calls;
- never persist raw secrets, embeddings, full source bodies, or chain-of-thought.

Ask mode continues deterministic pre-retrieval. Agent mode may call `search_knowledge` iteratively.
When the selected model does not support tools, Ask mode remains functional and Agent mode fails with
a clear unsupported-capability state instead of pretending execution.

### Agent execution state

Add the smallest durable state necessary for one read-only agent run:

```text
QUEUED -> PLANNING -> RUNNING -> VERIFYING -> COMPLETED
                            \-> FAILED | CANCELLED | BLOCKED
```

- Persist run id, user, conversation, assistant message, mode, state, iteration/tool limits,
  correlation id, timestamps, stop reason, and safe summary.
- Persist typed step/tool records separately if one run can have multiple events.
- Reopening another chat must not cancel the run. Returning to the conversation must recover its
  persisted state and final result.
- Do not claim resumability across a backend restart unless intermediate model/tool state can really
  resume. A restart may terminally fail an active run with the existing honest shutdown semantics.

## Phase 7 — SSE and frontend evidence

Add typed events without breaking existing ones:

- `capabilities`
- `retrieval_started`
- `retrieval_completed`
- `tool_started`
- `tool_completed`
- `tool_denied`
- `agent_state`

Every DTO remains a separate top-level record. Event payloads use safe public data only.

Frontend requirements:

- show selected Vaults as durable conversation context;
- show `No Vault sources used` on answers with no citations;
- show actual source chips when citations exist;
- show a compact tool activity row while `search_knowledge` runs;
- show completion/failure/denial with elapsed time;
- restore active/terminal agent state after navigation or reload;
- keep page layout fixed-height with scrolling inside the relevant chat component;
- do not expose raw prompt, embeddings, internal ids unnecessarily, or hidden chain-of-thought.

## Phase 8 — Tests and acceptance matrix

### Backend unit/contract tests

- prompt resources load and render deterministically;
- capability envelope accurately distinguishes selected/retrieved/none/unavailable;
- two users with different endpoints/models remain isolated;
- Spring AI message mapping preserves roles/order/system messages;
- chat streams answer and thinking separately;
- usage and finish reason are captured;
- explicit cancellation stops Spring AI streaming and persists partial content;
- browser disconnect does not cancel execution;
- missing/invalid provider produces the existing safe failure contract;
- embeddings use Spring AI and preserve order/dimensions;
- cross-user Vault selection is denied before retrieval;
- selected Vaults persist by conversation;
- no selected Vault means no retrieval and an explicit capability statement;
- deterministic RAG delivers exactly the persisted citations;
- `search_knowledge` cannot escape the selected authorized Vault set;
- tool round, call, token, and result limits terminate honestly;
- unsupported tool model never records a fake tool execution;
- no chain-of-thought, vector, secret, or absolute path is persisted/exposed.

### Integration tests

- deterministic fake Ollama exercises Spring AI `/api/chat` streaming and `/api/embed` batch calls;
- PostgreSQL isolation test covers conversation selection plus pgvector retrieval;
- full application context starts with Spring AI 2.0 and dynamic provider construction;
- existing session/CSRF boundaries remain unchanged.

### Frontend tests

- Vault toggle persists and restores per conversation;
- pending/error selection state is visible;
- citation and no-source labels render correctly;
- tool/agent events parse through Zod and render compactly;
- navigating away/back restores active run state;
- existing model picker, cancellation, token usage, message history, and responsive layout remain
  functional.

### Required commands

Run, fix, and report:

```bash
cd backend
./mvnw -Dexcluded.test.groups=ollama,docker verify

cd ../frontend
npm test -- --run
npm run build

cd ..
git diff --check
```

When Docker is available, also rebuild/start the backend and verify Flyway plus application startup
on Java 25/PostgreSQL/pgvector. Run the real Ollama smoke test only when the configured models are
available; otherwise report that precise residual check.

## Phase 9 — Documentation and cleanup

Update at minimum:

- `docs/DECISIONS.md`: supersede D-021's direct-protocol choice with the measured Spring AI 2.0
  integration and document why Nexo still owns authorization/persistence.
- `docs/TECH_STACK.md`: describe the actual Spring AI contracts now used.
- `docs/IMPLEMENTATION_STATUS.md`: record the delivered behavior and exact test totals.
- `docs/RAG_ARCHITECTURE.md`: explain deterministic RAG plus the governed knowledge tool.
- `docs/CONTEXT_AND_SKILL_GOVERNANCE.md`: document capability envelope and tool scoping.
- `docs/AGENT_CAPABILITIES.md`: record the first read-only agent/runtime boundary and deferred tools.
- generated documentation data through `node scripts/build-docs-data.mjs`.

Delete obsolete manual protocol code and correct stale comments/tests. Do not leave two active chat or
embedding implementations for Ollama. Do not claim general autonomous agent support: only the
implemented read-only knowledge tool and durable agent lifecycle are complete.

## Definition of done

The work is complete only when all statements below are true:

1. Production chat and embedding paths import and execute Spring AI 2.0 contracts.
2. No production code manually parses Ollama chat NDJSON or manually calls `/api/embed`.
3. Per-user provider endpoint/model isolation remains proven by tests.
4. Nexo identity and rules come from backend Markdown resources.
5. Every request contains a truthful typed capability envelope.
6. Vault selection survives chat switching and page reload.
7. Answers clearly distinguish citations from no-source responses.
8. The read-only `search_knowledge` tool executes only within authorized selected Vaults.
9. Tool/agent activity is visible, persisted, bounded, auditable, cancellable, and recoverable after
   frontend navigation.
10. Existing SSE, cancellation, usage, and conversation behavior remains compatible.
11. Backend tests, frontend tests, frontend build, and `git diff --check` pass.
12. Documentation describes reality and explicitly lists deferred write tools/MCP/multi-agent work.

## Required Claude Code handoff report

When implementation is finished, report:

- architectural changes by module;
- files added/removed;
- migrations and compatibility impact;
- exact tests/commands and totals;
- any model-specific Spring AI limitation observed;
- any acceptance criterion not completed and why;
- `git status --short`;
- confirmation that `.claude/` was not edited, staged, or deleted;
- confirmation that no commit or push was created.
