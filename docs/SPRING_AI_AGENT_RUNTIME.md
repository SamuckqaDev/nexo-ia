# Spring AI 2.0 Agent runtime

Nexo uses Spring AI 2.0.1 as the orchestration layer for Ollama chat, embeddings, Advisors, and
request-scoped tools. This is a real but deliberately bounded Agent runtime: the selected model can
revise a visible implementation plan, search the conversation's authorized Knowledge Vaults, and
call explicitly enabled tools from the authenticated user's MCP registry. It cannot yet read a
Workspace, edit files, run arbitrary terminal commands, write Git state, or delegate to subagents.

## Source-backed framework choices

The implementation was checked against the current
[Spring AI Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html),
[ChatClient reference](https://docs.spring.io/spring-ai/reference/api/chatclient.html),
[Advisors reference](https://docs.spring.io/spring-ai/reference/api/advisors.html), and
[RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html).
Spring AI 2.0 moves the tool loop into the `ChatClient` advisor chain: `ToolCallingAdvisor` asks the
model, `ToolCallingManager` executes an attached callback, the result becomes a tool response, and
the advisor repeats until the model returns an ordinary answer or a limit stops the run.

Ollama model compatibility is read from the capability metadata reported by the local catalog, with
the official [`/api/show`](https://docs.ollama.com/api-reference/show-model-details) endpoint as a
fallback. Only a model advertising `tools` is presented as **Agent ready**; the behavior follows
Ollama's [tool-calling contract](https://docs.ollama.com/capabilities/tool-calling).

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
  -> ToolCallingAdvisor drives the bounded loop
       update_plan       always in Agent mode
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
| Visible plan | None | Persisted `update_plan` revisions |
| Tool loop | None | Spring AI `ToolCallingAdvisor` |
| External MCP tools | None | Explicitly selected, governed callbacks |
| Native write/system tools | None | None |

The composer remains writable in Agent mode and includes a compact **Agent context** inspector. It
shows the real conversation Vault selection, enabled MCP server/tool count, loading or failure
states, and the selected model's tool compatibility before a request is sent. The Knowledge bar and
composer share the same bounded width. A model explicitly reporting no tool calling keeps the
textarea editable but blocks the invalid Agent submission and points the user back to the model
picker; unknown capability metadata remains usable with an explicit warning.

For an Agent request with selected Vaults, the initial capability envelope now says that Knowledge
is `available_on_demand` instead of incorrectly describing it as not requested. It tells the model
to call `search_knowledge` for a focused query. Enabled `mcp_*` names are separately identified as
callable external tools, while tool results remain the only acceptable evidence of execution.

`update_plan` replaces the complete visible plan. It accepts at most twelve concise steps, allows at
most one `IN_PROGRESS` step, rejects identical repeats, and is capped at eight calls per request.
`search_knowledge` accepts only a bounded query and result count, searches only server-captured Vault
scope, rejects repeated identical queries, and is capped at three calls. MCP tools share a six-call
request cap, two calls per tool, duplicate-argument denial, bounded output, evidence, audit, and
cancellation. `ToolCallingManager` caps the combined loop and throws on exhaustion rather than
looping forever. See [MCP runtime and implementation plan](MCP_RUNTIME.md).

## Knowledge and isolation

Vault selection is durable conversation state. The server authorizes every selected Vault before it
is stored and resolves the authoritative selection again for each request. Model-facing tool schemas
contain no user id, owner id, Vault id, endpoint, SQL, or filesystem path. Retrieval joins
chunk → source → Vault and filters owner plus selected Vaults before vector ranking. Excerpts are
bounded and treated as untrusted reference context; vectors and full source bodies never leave the
backend.

## Identity, plans, and reasoning

The Nexo identity, general rules, Agent rules, capability framing, and Knowledge framing live in
`backend/src/main/resources/prompts/*.md`. Every request says which model and tools were actually
provided. Agent planning stores concise steps and status, not private chain-of-thought. Provider
reasoning remains opt-in, transient, separate from answer content, and absent from persistence and
future context.

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
