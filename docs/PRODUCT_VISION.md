# Product vision

## Product statement

Nexo IA is a local-first AI workspace that helps a developer understand information, work with
projects, and safely use tools while keeping data and decisions under the user's control.

## Primary user

The first user is a developer who wants to:

- learn how modern AI assistants work;
- talk to local language models;
- ask questions about personal documents and source code;
- delegate controlled actions to an agent;
- inspect what the model knew, selected, and executed;
- retain ownership of data, models, and tools.

Nexo IA is initially a single-user product. Multi-user and public-server requirements must not shape
the first architecture.

## Problems to solve

### Local AI is fragmented

Models, documents, prompts, vector stores, and tools often require separate applications and manual
configuration. Nexo IA should provide one understandable workspace for them.

### Agent actions are difficult to trust

An LLM can request an unsafe, irrelevant, or malformed action. Nexo IA must put a deterministic control
layer between model intent and real-world effects.

### RAG can sound correct without being correct

Retrieval alone does not guarantee a grounded answer. Nexo IA must display sources and evaluate both
retrieval and answer quality.

### AI systems hide too much

Users often cannot tell which model, context, memory, or tool produced a result. Nexo IA should make
important execution details inspectable without overwhelming the normal conversation.

## Core user journeys

### 1. Talk to a local model

The user selects an installed model, starts a conversation, receives a streamed answer, and can see
which model and settings produced it.

### 2. Ask questions about knowledge

The user creates a knowledge collection, adds documents, waits for ingestion, and asks questions.
Nexo IA answers with links to the supporting passages or clearly states that evidence was not found.

### 3. Work with a project

The user authorizes a workspace. Nexo IA can inspect it with read-only tools, propose a plan, request
permission for changes, show a diff, run validation, and report evidence.

### 4. Extend Nexo IA through MCP

The user registers an MCP server, reviews its capabilities and permissions, enables selected tools,
and uses them through the same safety rules as native tools.

### 5. Understand an agent run

The user can inspect a timeline containing model rounds, tool requests, permission decisions,
results, errors, token usage, and the reason an execution stopped.

### 6. Work together in Cowork

The user opens a Cowork session with a concrete objective, selects the relevant workspace and
knowledge, and collaborates with Nexo IA through a visible plan. Nexo IA researches, proposes steps,
executes approved actions, adapts to feedback, and delivers a verifiable result with a summary of
what changed.

### 7. Delegate scheduled work

The user creates an automation with a clear objective, schedule, timezone, authorized context,
allowed capabilities, limits, and an expected output. Nexo IA executes it unattended within that
pre-approved scope, records the full run, and notifies the user of success, failure, or a decision
that requires human input.

## Product boundaries

- The model generates proposals; application code grants capabilities.
- A workspace is unavailable until the user explicitly authorizes it.
- RAG sources are separate from conversation history and long-term memory.
- Native and MCP tools follow one permission model.
- A final answer must not claim an action succeeded without execution evidence.
- Local-first does not mean offline-only: remote providers may be added through explicit opt-in.

## Interaction modes

### Chat

Best for questions, explanations, brainstorming, and short requests. Chat may use knowledge and
read-only tools, but it does not imply an autonomous multi-step execution.

### Cowork

Best for goals that require planning, multiple actions, user decisions, and a concrete deliverable.
A Cowork session has its own objective, context, plan, artifacts, activity timeline, and completion
state.

### Agent run

An agent run is an execution mechanism, not a user-facing workspace. Chat or Cowork may start a run,
but Cowork coordinates multiple runs and user interactions around one durable objective.

### Automations

Best for recurring or future work that should run without the user being present. An automation is a
durable definition that creates isolated agent runs on a schedule. It never inherits unrestricted
permissions from a previous Chat or Cowork session.

### Skills

Best for repeatable methods such as project analysis, research, code review, document processing, or
automation setup. A skill provides workflow knowledge and output requirements; it does not bypass
tool permissions or execution limits.
