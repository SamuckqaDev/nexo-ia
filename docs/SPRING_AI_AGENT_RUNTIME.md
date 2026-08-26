# Spring AI 2.0 Agent runtime

Nexo uses Spring AI 2.0.1 as the orchestration layer for Ollama chat, embeddings, Advisors, and
request-scoped tools. This is a real but deliberately bounded Agent runtime: the selected model can
revise a visible implementation plan, search the conversation's authorized Knowledge Vaults, store
an explicitly requested personal memory, and call explicitly enabled tools from the authenticated
user's MCP registry. It cannot yet read a
Workspace, edit files, run arbitrary terminal commands, write Git state, or delegate to subagents.

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
  -> lock and reserve user/assistant messages
  -> resolve user Provider, model, conversation, and selected Vaults
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
| Native write/system tools | None | None |

The composer remains writable in Agent mode and includes a compact **Agent context** inspector. The
Chat/Agent selection persists across navigation, so visiting the MCP Hub cannot silently downgrade
the next request to Chat mode. It
shows the real conversation Vault selection, enabled MCP server/tool count, loading or failure
states, and the selected model's tool compatibility before a request is sent. The Knowledge bar and
composer share the same bounded width. A model explicitly reporting no tool calling keeps the
textarea editable but blocks the invalid Agent submission and points the user back to the model
picker; unknown capability metadata remains usable with an explicit warning.

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
**Activity** section follows `agent_state`, `plan_updated`, `tool_started`, and `tool_completed`, then
restores the persisted plan and sanitized evidence after navigation or reload. It shows timestamps,
duration, terminal status, and safe citations without exposing raw arguments, secrets, or private
chain-of-thought. The assistant turn keeps only a compact status/action summary that points to the
Activity panel.

Explicit requests to consult selected Vaults, persist personal memory, or perform external MCP
research are evidence-gated. The runtime narrows the callback set to the required tools, buffers the
model's prose, and releases an answer only after each required action has successful tool evidence.
If a required callback is absent, ignored, denied, unavailable, or failed, Nexo returns a controlled
runtime result instead of accepting a model-authored success claim. Capability inspection itself is
also emitted and persisted as a visible `inspect_capabilities` Activity event.

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

- Workspace/file read and write tools;
- native terminal, Git, browser, email, and database tools;
- MCP credentials/OAuth, configuration-dependent Docker entries, resources/prompts, and a Companion
  bridge for a containerized backend;
- approval gates and reversible write transactions;
- resumable intermediate tool/model state across backend restarts;
- evaluator/optimizer loops and multi-agent orchestrator/worker execution;
- authored Vault backlinks, wikilinks, and relationship-aware graph expansion.

Each future tool must be attached per request after deterministic authorization and must reuse the
same limits, sanitized evidence, typed events, cancellation, and audit contracts.
