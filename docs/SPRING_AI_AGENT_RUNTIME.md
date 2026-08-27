# Spring AI 2.0 Agent runtime

Nexo uses Spring AI 2.0.1 as the orchestration layer for Ollama chat, embeddings, Advisors, and
request-scoped tools. This is a real but deliberately bounded Agent runtime: the selected model can
revise a visible implementation plan, search the conversation's authorized Knowledge Vaults, store
an explicitly requested personal memory, and call explicitly enabled tools from the authenticated
user's MCP registry. With a server or online Nexo Desktop Workspace selected and `WORKSPACE_READ`
allowed, it can also list
and search project files, read safe text excerpts, inspect project metadata, and read Git status or a
one-file diff. For a direct and explicit Agent request, it can also create or replace one bounded
Workspace file through `workspace_write_file`; this is not a shell and does not grant standing write
access. It cannot run arbitrary terminal commands, write Git state, or delegate to workers.

## Source-backed framework choices

The implementation was checked against the current
[Spring AI Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html),
[Tool Search reference](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools/tool-search-tool.html),
[ChatClient reference](https://docs.spring.io/spring-ai/reference/api/chatclient.html),
[Advisors reference](https://docs.spring.io/spring-ai/reference/api/advisors.html), and
[RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html).
Spring AI 2.0 moves the tool loop into the `ChatClient` advisor chain. Nexo uses a hybrid advisor
strategy: catalogs with at most ten request-owned callbacks use `ToolCallingAdvisor` and disclose
those authorized schemas directly; larger catalogs use `ToolSearchToolCallingAdvisor` for progressive
discovery. Direct disclosure supports smaller local models that advertise tool calling but do not
reliably invoke the intermediate `toolSearchTool`.
`ToolCallingManager` executes the callback, its result becomes a tool response, and the advisor
repeats until the model returns an ordinary answer or a limit stops the run.

Ollama model compatibility is read from the capability metadata reported by the local catalog, with
the official [`/api/show`](https://docs.ollama.com/api-reference/show-model-details) endpoint as a
fallback. Only a model advertising `tools` is presented as **Agent ready**; the behavior follows
Ollama's [tool-calling contract](https://docs.ollama.com/capabilities/tool-calling). Thinking is a
separate advertised capability: the model picker identifies it independently, and Nexo does not send
the opt-in Thinking request when the selected model explicitly reports that it is unsupported.

Baeldung's guides on
[recursive Advisors](https://www.baeldung.com/spring-ai-recursive-advisors) and
[effective Agent patterns](https://www.baeldung.com/spring-ai-building-effective-agents) informed
the bounded-loop and visible-workflow design. Some Baeldung examples target Spring AI 1.x/1.1 and use
older names, so the production API follows the official 2.0.1 reference when contracts differ.

## Runtime flow

```text
authenticated message
  -> resolve the effective objective, including terse confirmations
  -> promote Workspace operations from Chat to Agent when execution is required
  -> select a request-local tool-capable executor when the chosen model has no tools
  -> lock and reserve user/assistant messages with the effective mode and actual executor
  -> resolve user Provider, conversation, and selected Vaults
  -> assemble Markdown identity + rules + truthful capability envelope
  -> build request-local OllamaChatModel and ChatClient
  -> SpringAiContextAdvisor injects the authorized system context
  -> a Spring AI tool advisor drives the bounded execution loop
       direct schemas     up to ten callbacks for reliable local-model invocation
       toolSearchTool     progressive discovery above ten callbacks
       inspect_capabilities exact safe catalog for the current authenticated request
       update_plan       always in Agent mode
       remember          personal memory, owned by the authenticated account
       search_knowledge  only with authorized selected Vaults
       mcp_*              only from the owner's enabled, explicitly selected MCP snapshot
       workspace_*        only for the persisted, authorized server or Desktop binding
  -> stream typed Agent/tool/plan/token/usage events
  -> persist answer, plan, citations, tool evidence, usage, and terminal state
```

The browser SSE connection is an observer, not the execution owner. Leaving the chat stops writes to
that connection but does not cancel the server run. Reopening the conversation restores the stored
Agent state, latest plan revision, and tool evidence. Explicit cancellation remains authoritative.

## Chat and Agent modes

| Behavior | Chat | Agent |
| --- | --- | --- |
| Conversation history | Bounded server history | Bounded server history |
| Selected Vaults | Deterministic retrieval before generation | Available through `search_knowledge` |
| Personal memory | Most recent owned memories in context | Same context plus explicit `remember` tool |
| Visible plan | None | Persisted `update_plan` revisions |
| Tool loop | None | Spring AI direct or progressive advisor, selected by catalog size |
| External MCP tools | None | Explicitly selected, governed callbacks |
| Server/Desktop Workspace reads | None | Conditional bounded file/search/Git inspection callbacks |
| Governed Workspace file write | None | Explicit request only; bounded, audited, SHA-protected callback |
| Native command/system tools | None | None |

The composer remains writable in Agent mode and includes a compact **Agent context** inspector. The
Chat/Agent selection persists across navigation, so visiting the MCP Hub cannot silently downgrade
the next request to Chat mode. It
shows the real conversation Vault selection, enabled MCP server/tool count, loading or failure
states, and the selected model's tool compatibility before a request is sent. The Knowledge bar and
composer share the same bounded width. A model explicitly reporting no tool calling no longer turns
an execution request into a tutorial and no longer blocks the composer. The server selects a
request-local Agent-ready model from the same provider, prefers Thinking support when the preference
requires it, and persists/emits that actual executor without changing the conversation's preferred
model. If the provider has no tool-capable model, Nexo rejects the request before inference and does
not spend tokens on fabricated commands. Unknown capability metadata remains usable with an explicit
warning.

A message does not have to begin in the visual Agent toggle to receive execution semantics. When an
authorized conversation has a selected Workspace and the effective objective asks to inspect,
search, analyze, read, create, or change that project, deterministic server code promotes the request
from Chat to Agent before reservation. Ordinary conversation remains Chat. This closes the gap where
the model previously promised to act, printed `cat`/JSON instructions, and completed without any
tool evidence.

The model picker does not equate Agent support with Thinking support. For example, a model may
advertise tool calling and therefore be Agent-ready while explicitly lacking provider reasoning.
When the user's Thinking preference is enabled for such a model, the composer says **no Thinking**
instead of presenting an ordinary fast answer as hidden reasoning.

For an Agent request with selected Vaults, the initial capability envelope now says that Knowledge
is `available_on_demand` instead of incorrectly describing it as not requested. It tells the model
to call `search_knowledge` for a focused query. Enabled `mcp_*` names are separately identified as
callable external tools, while tool results remain the only acceptable evidence of execution.

Every Agent execution resolves a fresh callback list for the authenticated request. With ten or
fewer callbacks, the first provider call contains their actual schemas, including
`inspect_capabilities`; above ten, a fresh regex index is keyed with the server-created assistant
message id and the first provider call contains only `toolSearchTool`. After a search, only matching
definitions are disclosed. `inspect_capabilities` returns the exact safe names and
descriptions from that same callback list, so questions such as “which tools can you use?” can be
answered from runtime state instead of prompt claims. Ownership ids, Vault ids, endpoints, secrets,
and raw MCP configuration are never part of that result.

Capability-list questions are answered deterministically from that request's actual callback
snapshot instead of asking the selected model to repeat the catalog. For explicit web research or
URL access with enabled MCP scope, Nexo removes stale assistant refusals from the provider turn,
requires the next model action to call one of the attached `mcp_*` callbacks, and buffers answer
text until persisted MCP execution evidence exists. A model response that merely claims that MCP is
unavailable is discarded and the request fails in a controlled way; it can never be persisted as a
successful researched answer. If an enabled connection produces no callable callback, Nexo reports
that runtime condition directly and points the user to the MCP Hub.

`update_plan` replaces the complete visible plan. It accepts at most twelve concise steps, allows at
most one `IN_PROGRESS` step, rejects identical repeats, and is capped at eight calls per request.
Each step contains a short title plus an observable description, so the user can tell what result
will prove the step complete. Nexo publishes a deterministic decomposition of the actual user
objective as soon as every Agent request starts. A model may replace it through `update_plan`; if the
model returns a normal answer without doing so, Nexo completes only the fallback steps supported by
the runtime evidence. A Vault lookup, memory write, or MCP action stays pending when the corresponding
tool never completed successfully.

The newest revision is rendered in the conversation workspace's **Plan** section. A separate
**Tasks** section follows `agent_state`, `plan_updated`, `tool_started`, and `tool_completed`, then
restores the persisted plan and sanitized evidence after navigation or reload. It shows timestamps,
duration, terminal status, and safe citations without exposing raw arguments, secrets, or private
chain-of-thought. The assistant turn keeps only a compact status/action summary that points to the
Tasks panel.

Explicit requests to consult selected Vaults, persist personal memory, or perform external MCP
research are evidence-gated. The runtime narrows the callback set to the required tools, buffers the
model's prose, and releases an answer only after each required action has successful tool evidence.
If a required callback is absent, ignored, denied, unavailable, or failed, Nexo returns a controlled
runtime result instead of accepting a model-authored success claim. Controlled tool failures are
persisted as `FAILED`, never as a completed Agent turn. Capability inspection itself is also emitted
and persisted as a visible `inspect_capabilities` task event.

Project-analysis requests have a deterministic server preflight because small local models may print
tool-call JSON as prose instead of using Spring AI's native tool protocol. Before the synthesis turn,
Nexo executes `workspace_list_files`, `workspace_inspect_project`, and `workspace_git_status`, then
reads the first recognized project manifest and README when present. Each call emits normal Tasks,
evidence, and audit records. The model receives bounded real results without tool schemas and must
produce the completed analysis rather than a future-tense plan. A short `continue`/`continua` resumes
the preceding project-analysis intent and runs the same governed preflight.

Explicit Workspace write requests have a separate mandatory evidence gate. Nexo resolves terse
continuations such as **faça**, **execute**, **continue**, or **“faça isso pra mim, pode criar”**
against the latest unresolved user
objective before planning or authorizing tools. The selected Skill remains supporting context and
cannot turn the objective into a different task. When authorized, the runtime discloses
`workspace_write_file` and buffers any success answer until that callback returns completed evidence.
Printing HTML, a shell command, or tool-call-shaped JSON is treated as prose, not execution. A model
that ignores or cannot call the tool produces a controlled failed Agent run instead of a false
completion. Existing files must first be read so their current SHA-256 can be supplied to the write.

Knowledge-only requests have an additional output boundary because tool execution alone does not
prove that the model attributed the result correctly. Nexo buffers the answer, accepts a URL only
when the exact URL occurs in a returned Vault excerpt, and replaces unsupported links with a bounded
summary built directly from the persisted citations. A successful search with zero citations always
returns an explicit no-evidence result instead of allowing model recall to fill the gap.

`toolSearchTool` is capped at three calls and `inspect_capabilities` at two calls per request. These
internal discovery calls are included in the combined tool-call budget. `search_knowledge` accepts
only a bounded query and result count, searches only server-captured Vault
scope, rejects repeated identical queries, and is capped at three calls. MCP tools share a six-call
request cap, two calls per tool, duplicate-argument denial, bounded output, evidence, audit, and
cancellation. `ToolCallingManager` caps the combined loop and throws on exhaustion rather than
looping forever. See [MCP runtime and implementation plan](MCP_RUNTIME.md).

`remember` is attached only in Agent mode and is capped at two calls per request. The model-facing
schema contains only the memory text: user, conversation, assistant message, and correlation IDs are
captured by the server-created request scope. Duplicate text is reused instead of copied, each user
is limited to 50 personal memories, and at most the 20 most recently updated owned memories are
framed as untrusted personal context for later Chat or Agent requests. The conversation workspace's
**Memory** section lets the authenticated person inspect and delete this first slice. Automatic
extraction, semantic selection, editing, expiration, and shared scopes remain deferred.

Workspace callbacks are attached from the conversation's persisted `workspaceId`, never from a
browser-local active-folder value. Permission resolution must allow `WORKSPACE_READ`, and every call
reauthorizes ownership and live availability. The read callbacks and optional governed write
callback share a 12-call request cap,
duplicate-argument denial, sanitized evidence and audit. Explicit project/repository inspection is
evidence-gated in the same way as MCP, Vault, and memory work. See
[Server workspaces](SERVER_WORKSPACES.md).

The resolved content matrix is authoritative for Nexo, but an individual provider model may still
have its own non-configurable refusal behavior. When a model falsely attributes a refusal to Nexo for
a lawful area resolved as `FULL`, the output guard replaces that claim with an honest distinction:
the Nexo profile allowed the request, the selected model refused it, and no media/action was executed.
This does not bypass the fixed legal floor or force a provider to generate unsupported content.

## Knowledge and isolation

Vault selection is durable conversation state. The server authorizes every selected Vault before it
is stored and resolves the authoritative selection again for each request. Model-facing tool schemas
contain no user id, owner id, Vault id, endpoint, SQL, or filesystem path. Retrieval joins
chunk → source → Vault and filters owner plus selected Vaults before vector ranking. Excerpts are
bounded and treated as untrusted reference context; vectors and full source bodies never leave the
backend.

Personal memory is separate from history and Vault knowledge. Repository queries are always filtered
by the authenticated user, provenance records the source conversation/message when the Agent created
the memory, and memory text is framed as context rather than instructions or authorization. Deleting
a memory removes it from subsequent request assembly.

## Identity, plans, and reasoning

The Nexo identity, general rules, Agent rules, capability framing, and Knowledge framing live in
`backend/src/main/resources/prompts/*.md`. Every request says which model and tools were actually
provided. Agent planning stores concise steps and status, not private chain-of-thought. Provider
reasoning remains opt-in, transient, separate from answer content, and absent from persistence and
future context. If a thinking-capable model completes with reasoning but no final answer, Nexo
retries once without thinking and combines provider usage. It never persists an empty successful
answer, and it does not repeat a completed tool effect merely to recover missing prose.

## Deliberately deferred

- arbitrary command execution, Git mutation, and multi-file write transactions;
- native terminal, Git, browser, email, and database tools;
- MCP credentials/OAuth, configuration-dependent Docker entries, resources/prompts, and a Companion
  bridge for a containerized backend;
- persistent/interactive approval records and reversible write transactions beyond the current
  fresh-request plus SHA-256 write guard;
- resumable intermediate tool/model state across backend restarts;
- evaluator/optimizer loops and multi-agent orchestrator/worker execution;
- authored Vault backlinks, wikilinks, and relationship-aware graph expansion.

Each future tool must be attached per request after deterministic authorization and must reuse the
same limits, sanitized evidence, typed events, cancellation, and audit contracts.
