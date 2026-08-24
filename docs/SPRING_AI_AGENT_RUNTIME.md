# Spring AI 2.0 Agent runtime

Nexo uses Spring AI 2.0.1 as the orchestration layer for Ollama chat, embeddings, Advisors, and
request-scoped tools. This is a real but deliberately bounded Agent runtime: the selected model can
revise a visible implementation plan and search the conversation's authorized Knowledge Vaults. It
cannot yet read a Workspace, edit files, run a terminal, write Git state, browse, call MCP servers,
or delegate to subagents.

## Source-backed framework choices

The implementation was checked against the current
[Spring AI Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html),
[ChatClient reference](https://docs.spring.io/spring-ai/reference/api/chatclient.html),
[Advisors reference](https://docs.spring.io/spring-ai/reference/api/advisors.html), and
[RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html).
Spring AI 2.0 moves the tool loop into the `ChatClient` advisor chain: `ToolCallingAdvisor` asks the
model, `ToolCallingManager` executes an attached callback, the result becomes a tool response, and
the advisor repeats until the model returns an ordinary answer or a limit stops the run.

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
| Write/system tools | None | None |

`update_plan` replaces the complete visible plan. It accepts at most twelve concise steps, allows at
most one `IN_PROGRESS` step, rejects identical repeats, and is capped at eight calls per request.
`search_knowledge` accepts only a bounded query and result count, searches only server-captured Vault
scope, rejects repeated identical queries, and is capped at three calls. `ToolCallingManager` caps the
combined request at eleven tool calls and throws on limit exhaustion rather than looping forever.

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

- model-capability discovery before enabling Agent mode;
- Workspace/file read and write tools;
- terminal, Git, browser, email, database, and external MCP tools;
- approval gates and reversible write transactions;
- resumable intermediate tool/model state across backend restarts;
- evaluator/optimizer loops and multi-agent orchestrator/worker execution;
- authored Vault backlinks, wikilinks, and relationship-aware graph expansion.

Each future tool must be attached per request after deterministic authorization and must reuse the
same limits, sanitized evidence, typed events, cancellation, and audit contracts.
