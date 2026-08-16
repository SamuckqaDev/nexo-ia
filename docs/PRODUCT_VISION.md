# Product vision

## Product statement

Nexo IA is a local-first, team-ready AI workspace that helps people understand information, work
with projects, and safely use tools while keeping data and decisions under governed control.

## Primary users

The first users are individuals and engineering or creative teams that want to:

- learn how modern AI assistants work;
- talk to local language models;
- ask questions about personal documents and source code;
- delegate controlled actions to an agent;
- inspect what the model knew, selected, and executed;
- retain ownership of data, models, and tools.

Nexo IA is designed as a multi-user organization product, while remaining useful as a one-person
local installation. Its core behavior and Companion run on Linux, Windows, and macOS; a central
server or DGX may coordinate work performed on paired user devices.

The first release proves the foundation with two isolated users, organization-owned Ollama,
persistent streaming chat, usage attribution, and audit. Later capabilities build on that verified
slice rather than entering the MVP simultaneously.

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

The user creates or connects a Knowledge Vault, adds interlinked notes and documents, waits for
ingestion, and asks questions. Nexo IA combines semantic, textual, metadata, and relationship-aware
retrieval, then answers with links to original passages or states that evidence was not found.

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

### 8. Review work in the calendar

The user opens a unified month, week, day, or agenda view containing scheduled automations, Cowork
milestones, checkpoints, approval deadlines, and their current states. From an event, the user can
inspect its permissions and history, run a safe preview, execute it now, or change its schedule
without silently expanding its access.

### 9. Work with the computer

The user asks Nexo IA to organize files, open an application, run a development command, inspect a
process, interact with an authorized desktop application, or complete another supported system task.
Nexo IA explains the intended effect, requests the required permission, uses the adapter for the
current operating system, and verifies the observable result.

### 10. Work safely with a Project database

The user selects an explicit Project connection and environment, asks Nexo IA to inspect or change
data or schema, and reviews a plan containing impact, affected rows, recovery, migration, approval,
and validation. Nexo IA performs only granted database capabilities and commits a change only after
the declared checks pass.

### 11. Inspect Knowledge Vaults

The user opens personal, shared, team, or organization Vaults in the frontend and inspects their
sources, files, relationships, graph, tags, indexing status, permissions, activity, and citations.
Every operation shown by the Vault Explorer follows the user's current grants.

## Product boundaries

- The model generates proposals; application code grants capabilities.
- Multi-step or consequential work requires a visible execution plan before implementation effects.
- Approving a plan approves the strategy, not the concrete capabilities required to execute it.
- A workspace is unavailable until the user explicitly authorizes it.
- Knowledge Vault sources are separate from Workspaces, conversation history, and long-term memory,
  even when they reference the same Project or directory.
- Personal Memory may follow its owner across authorized chats; team and organization Memory requires
  explicit promotion, scope, provenance, and access.
- User-created Skills have ownership and visibility but no inherited access; every dependency is
  authorized again for the current user and run.
- Native and MCP tools follow one permission model.
- A final answer must not claim an action succeeded without execution evidence.
- Local-first does not mean offline-only: remote providers may be added through explicit opt-in.
- Cross-platform means a common capability contract with tested Linux, Windows, and macOS adapters;
  it does not mean assuming that shell commands, paths, applications, or permissions are identical.
- Unsupported platform capabilities fail explicitly and never fall back to a broader action.
- Project database access is separate from Nexo IA persistence; read and mutation capabilities are
  independent, environment-specific, recoverable, validated, and audited.

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

### Agent

Best for a large objective that can be decomposed and executed with limited interaction. Nexo IA
creates a visible plan, divides the Goal into milestones and verified tasks, executes ready work
incrementally, persists progress, and replans visibly when evidence changes. Permission decisions
remain separate from plan approval.

### Automations

Best for recurring or future work that should run without the user being present. An automation is a
durable definition that creates isolated agent runs on a schedule. It never inherits unrestricted
permissions from a previous Chat or Cowork session.

### Calendar

Best for understanding when work will happen and what needs attention. The calendar presents
persisted Cowork tasks and automation occurrences; it does not replace the scheduler or create a
separate source of truth. Schedule changes remain audited, timezone-aware, and independent from
permission changes.

### Skills

Best for repeatable methods such as project analysis, research, code review, document processing, or
automation setup. A skill provides workflow knowledge and output requirements; it does not bypass
tool permissions or execution limits.
