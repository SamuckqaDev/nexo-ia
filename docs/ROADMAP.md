# Learning and development roadmap

## Phase 0 — Foundations and identity

- Define the purpose, principles, scope, and identity.
- Select the technology stack using documented criteria.
- Prepare the repository, conventions, and testing strategy.

## Phase 1 — First contact with the LLM

- Learn the Ollama API.
- Send a message and interpret its response.
- Study tokens, messages, context, and generation parameters.
- Implement a minimal chat with streaming.

## Phase 2 — Persistent conversations

- Model conversations and messages.
- Persist history.
- Assemble context within a token budget.
- Test isolation between conversations.

## Phase 3 — RAG

- Ingest and split documents.
- Generate embeddings and retrieve passages.
- Display the sources used in responses.
- Create a question set to evaluate retrieval and answers.

## Phase 4 — Tools and permissions

- Define the tool contract.
- Implement read-only tools.
- Classify risks and request approval.
- Add scoped, audited write and execution capabilities.

## Phase 5 — MCP

- Study the protocol, transports, and lifecycle.
- Connect a simple MCP server.
- Discover tools progressively.
- Apply the same permission rules to MCP tools.

## Phase 6 — Agent

- Implement the think, act, observe, and respond loop.
- Limit rounds, time, tools, and context volume.
- Detect repetition and stop unproductive loops.
- Verify results before reporting completion.

## Phase 7 — Skills, instructions, and planning

- Define reusable skills with progressive instruction loading.
- Add explicit invocation, safe implicit activation, and skill tests.
- Layer personal, workspace, directory, and session instructions predictably.
- Add Plan, Goal, Build, Ask, and Review workflows without changing security boundaries.
- Expose a context inspector without revealing secrets or private model reasoning.

## Phase 8 — Memory and reliability

- Separate history, memory, and retrieved knowledge.
- Add tracing and metrics.
- Recover interrupted executions only when necessary.
- Create security, quality, and performance evaluations.

## Phase 9 — Cowork

- Create durable sessions around explicit objectives.
- Attach workspaces, knowledge, plans, runs, and artifacts to a session.
- Add human checkpoints and resumable task states.
- Produce evidence-backed completion reports.
- Add templates only after real workflows reveal reusable patterns.

## Phase 10 — Scheduled tasks and automations

- Persist one-time and recurring schedules with explicit timezones.
- Define pre-authorized capabilities, targets, budgets, and failure policies.
- Execute isolated, idempotent runs without an active user session.
- Add dry runs, manual tests, history, notifications, and human-review states.
- Add event triggers and chained workflows only after scheduled runs are safe and reliable.

## Phase 11 — Image generation

- Define a provider-independent image request and result contract.
- Integrate a local ComfyUI workflow through asynchronous, cancellable jobs.
- Preserve prompts, models, parameters, files, and provenance.
- Attach generated images to Chat, Cowork, Skills, and Automation results.
- Evaluate remote Spring AI image providers only through explicit opt-in.
