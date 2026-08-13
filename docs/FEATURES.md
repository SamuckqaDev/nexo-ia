# Feature catalog

This catalog describes intended capabilities, not completed work. Each feature has an identifier so
that requirements, decisions, tests, and commits can refer to it consistently.

## Priority levels

- **P0 — Foundation:** required for the first usable version.
- **P1 — Core:** required for the first complete learning cycle.
- **P2 — Expansion:** useful after the core is reliable.
- **Later:** intentionally postponed.

## Conversation and models

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| CHAT-01 | P0 | Local chat | Send messages to an Ollama model and render Markdown responses. |
| CHAT-02 | P0 | Response streaming | Display output incrementally and support cancellation. |
| CHAT-03 | P0 | Conversation history | Create, rename, list, reopen, and delete conversations. |
| CHAT-04 | P0 | Model selection | List installed models and preserve the selected model per conversation. |
| CHAT-05 | P1 | Context budget | Assemble history within an explicit token budget. |
| CHAT-06 | P1 | Generation controls | Configure supported parameters with safe defaults. |
| CHAT-07 | P2 | Provider abstraction | Add opt-in providers without changing tools or conversation rules. |

## Knowledge and RAG

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| RAG-01 | P1 | Knowledge collections | Group authorized documents into independently searchable collections. |
| RAG-02 | P1 | Document ingestion | Parse, chunk, embed, and index supported documents with visible status. |
| RAG-03 | P1 | Semantic retrieval | Retrieve relevant passages with scores and metadata. |
| RAG-04 | P1 | Grounded answers | Cite supporting passages and admit when evidence is insufficient. |
| RAG-05 | P1 | Evaluation set | Measure retrieval and answer quality with repeatable questions. |
| RAG-06 | P2 | Incremental updates | Re-index only documents whose content or configuration changed. |

## Workspaces and native tools

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| WORK-01 | P1 | Workspace authorization | Grant access only to an explicit directory and preserve its canonical path. |
| TOOL-01 | P1 | Tool contract | Validate structured inputs and return structured results and errors. |
| TOOL-02 | P1 | Read-only tools | List, read, and search authorized workspace content. |
| TOOL-03 | P1 | File changes | Propose changes, request permission, apply them, and present a diff. |
| TOOL-04 | P1 | Controlled commands | Execute approved commands with workspace, timeout, and output limits. |
| TOOL-05 | P2 | Change recovery | Record enough information to review and recover agent file changes. |

## Permission Engine

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| PERM-01 | P1 | Risk classification | Classify each capability by effect, target, and reversibility. |
| PERM-02 | P1 | Policy evaluation | Combine tool risk, requested scope, and user rules deterministically. |
| PERM-03 | P1 | Approval flow | Show the exact action, target, reason, and risk before approval. |
| PERM-04 | P1 | Scoped grants | Limit approval to one action, run, workspace, or explicitly chosen scope. |
| PERM-05 | P1 | Audit trail | Record requests, decisions, execution results, and ownership. |
| PERM-06 | P2 | Custom policies | Allow users to create stricter local policies with validation and previews. |

## MCP

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| MCP-01 | P1 | Server registry | Add, update, disable, and remove MCP server configurations. |
| MCP-02 | P1 | Lifecycle | Start, connect, health-check, reconnect, and stop supported servers. |
| MCP-03 | P1 | Capability discovery | Discover tools and expose only enabled capabilities to the agent. |
| MCP-04 | P1 | Unified permissions | Apply the Permission Engine to MCP and native tools consistently. |
| MCP-05 | P2 | Resources and prompts | Support MCP resources and prompts after tool integration is reliable. |

## Agent execution

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| AGENT-01 | P1 | Agent loop | Alternate model decisions and tool results until completion or a limit. |
| AGENT-02 | P1 | Execution limits | Enforce round, time, token, tool, and output limits. |
| AGENT-03 | P1 | Loop detection | Detect repeated equivalent calls and stop with an explanation. |
| AGENT-04 | P1 | Plans | Present a plan for multi-step or high-impact work. |
| AGENT-05 | P1 | Result verification | Require evidence before reporting that an action succeeded. |
| AGENT-06 | P2 | Durable runs | Recover interrupted executions without duplicating tool effects. |

## Skills, instructions, and planning

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| SKILL-01 | P1 | Skill format | Define metadata, triggers, instructions, resources, dependencies, and outputs. |
| SKILL-02 | P1 | Skill discovery | Find built-in, personal, and workspace skills with deterministic precedence. |
| SKILL-03 | P1 | Explicit invocation | Let users inspect and invoke a specific enabled skill by name. |
| SKILL-04 | P1 | Implicit activation | Match eligible skills by description and explain which skill was selected. |
| SKILL-05 | P1 | Progressive loading | Keep compact metadata in context and load full instructions only when selected. |
| SKILL-06 | P1 | Skill manager | Create, edit, validate, enable, disable, import, export, and remove skills. |
| SKILL-07 | P1 | Skill tests | Test positive, negative, incomplete, edge-case, and unsafe activation scenarios. |
| SKILL-08 | P2 | Skill packages | Install reviewed bundles containing skills, assets, and declared MCP dependencies. |
| INST-01 | P1 | Instruction scopes | Load personal, workspace, directory, and session instructions predictably. |
| INST-02 | P1 | Instruction inspector | Show active sources, precedence, conflicts, and truncation warnings. |
| PLAN-01 | P1 | Planning mode | Research and create a reviewable plan without implementation effects. |
| PLAN-02 | P1 | Plan editor | Approve, edit, reorder, pause, cancel, and explain revisions to plan steps. |
| GOAL-01 | P1 | Durable goals | Track outcome, constraints, verification, status, and remaining work. |
| GOAL-02 | P1 | Goal steering | Pause, resume, refine, cancel, or add context without losing progress. |
| MODE-01 | P1 | Interaction modes | Provide Ask, Plan, Build, Review, Cowork, and Automation workflows. |
| HOOK-01 | P2 | Lifecycle hooks | Run deterministic checks at declared lifecycle points with explicit failure behavior. |
| CTX-01 | P1 | Context inspector | Explain active model, instructions, skills, tools, knowledge, limits, and stop reason. |

## Cowork

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| COWORK-01 | P1 | Cowork sessions | Create a durable work session with a title, objective, status, and owner. |
| COWORK-02 | P1 | Context selection | Attach authorized workspaces, knowledge collections, and relevant conversations. |
| COWORK-03 | P1 | Collaborative plan | Create, review, approve, and update a visible sequence of work items. |
| COWORK-04 | P1 | Task states | Track pending, active, blocked, cancelled, and completed work explicitly. |
| COWORK-05 | P1 | Human checkpoints | Pause for decisions, clarification, or approval without losing session state. |
| COWORK-06 | P1 | Activity timeline | Combine messages, plan changes, runs, tool calls, approvals, and artifacts chronologically. |
| COWORK-07 | P1 | Deliverables | Collect created files, reports, diffs, links, and other outputs in one place. |
| COWORK-08 | P1 | Completion report | Summarize the outcome, evidence, changes, unresolved issues, and suggested next steps. |
| COWORK-09 | P2 | Templates | Start recurring workflows from editable objective, context, and checkpoint templates. |
| COWORK-10 | P2 | Resume and recovery | Continue an interrupted session safely from its persisted state. |

### Cowork safety rules

- A plan does not automatically grant permission to every action within it.
- Destructive or expanded-scope actions require a new explicit decision.
- Nexo IA must mark a session blocked instead of pretending that unavailable work was completed.
- Completion requires deliverables or execution evidence appropriate to the objective.
- The user can pause or cancel the session and revoke its access at any time.

## Scheduled tasks and automations

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| AUTO-01 | P1 | Automation definitions | Create, edit, duplicate, enable, disable, and delete scheduled tasks. |
| AUTO-02 | P1 | Scheduling | Support one-time and recurring schedules with an explicit timezone. |
| AUTO-03 | P1 | Execution scope | Bind each automation to an objective, model, context, workspace, tools, and limits. |
| AUTO-04 | P1 | Pre-authorized policy | Allow unattended actions only when they match capabilities and targets approved in advance. |
| AUTO-05 | P1 | Isolated runs | Create a separate run and audit trail for every scheduled occurrence. |
| AUTO-06 | P1 | Run history | Display scheduled time, start, duration, outcome, evidence, and resource usage. |
| AUTO-07 | P1 | Notifications | Report success, failure, skipped runs, and requests for human intervention. |
| AUTO-08 | P1 | Failure policy | Configure retry limits, backoff, timeout, and what happens after repeated failures. |
| AUTO-09 | P1 | Concurrency control | Prevent unsafe overlapping runs and define skip, queue, or replace behavior. |
| AUTO-10 | P1 | Manual run | Test or trigger the automation immediately under the same policy and limits. |
| AUTO-11 | P1 | Dry run | Preview context, planned capabilities, targets, and outputs without causing effects. |
| AUTO-12 | P2 | Event triggers | Start tasks from approved local events after scheduled execution is reliable. |
| AUTO-13 | P2 | Chained workflows | Start a subsequent automation from a verified result with independent limits. |

### Automation safety rules

- Every automation is disabled by default until its objective, schedule, scope, and policy are
  reviewed and explicitly enabled.
- An unattended run may use only the capabilities, targets, secrets, budget, and time limits granted
  to that automation.
- A model cannot expand its own permissions, modify its schedule, or enable the automation running it.
- A request outside the approved scope pauses or fails safely and creates a human-review item.
- Destructive actions are denied by default and require a specific, narrowly scoped policy.
- Credentials are referenced through a secret store and are never copied into prompts, logs, or
  automation definitions.
- Repeated failures automatically suspend the automation according to its failure policy.
- Each occurrence receives a unique run identifier and must be safe against duplicate delivery.

## Memory, transparency, and operations

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| MEM-01 | P1 | Explicit memory | Save a fact only through an observable user or agent decision. |
| MEM-02 | P1 | Memory controls | Inspect, edit, disable, and delete stored memories. |
| OBS-01 | P1 | Run timeline | Show model rounds, tool calls, approvals, results, and stop reasons. |
| OBS-02 | P1 | Usage metrics | Report latency, token estimates, retrieval timing, and tool duration. |
| OBS-03 | P1 | Structured logs | Correlate conversation, run, model, and tool events without logging secrets. |
| EVAL-01 | P1 | Evaluation harness | Run repeatable quality, safety, and regression scenarios. |
| DATA-01 | P1 | Data controls | Export and permanently delete conversations, documents, and memories. |

## Image generation

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| IMAGE-01 | P2 | Image generation | Generate images from a prompt through an enabled local or remote provider. |
| IMAGE-02 | P2 | Provider abstraction | Keep image requests independent from ComfyUI, Stability AI, or another provider. |
| IMAGE-03 | P2 | Generation options | Expose only supported size, format, seed, steps, guidance, model, and quality controls. |
| IMAGE-04 | P2 | Asynchronous jobs | Run long generations as cancellable jobs with queued, active, failed, and completed states. |
| IMAGE-05 | P2 | Local ComfyUI adapter | Submit reviewed workflows, track progress, and collect outputs from a local ComfyUI instance. |
| IMAGE-06 | P2 | Gallery and provenance | Store the prompt, model, provider, parameters, timestamps, and generated files together. |
| IMAGE-07 | P2 | Conversation integration | Attach generated images to Chat, Cowork, Skills, and Automation results. |
| IMAGE-08 | P2 | Safety and permissions | Apply output-path, model, provider, resource, and remote-data policies before execution. |
| IMAGE-09 | Later | Image editing | Edit an authorized source image while preserving source and generation provenance. |

Image generation is part of the product, but not part of the first chat or RAG milestone. Local
generation through ComfyUI is the preferred learning path. Spring AI's `ImageModel` abstraction may
support compatible remote providers, while ComfyUI remains behind a Nexo IA-owned adapter.

## Intentionally postponed

| ID | Priority | Feature | Reason |
|---|---|---|---|
| VIDEO-01 | Later | Video generation | Adds substantial model, storage, and job complexity after images. |
| VOICE-01 | Later | Speech input and output | Adds runtime and interface complexity before the core is stable. |
| DESKTOP-01 | Later | Desktop automation | Requires a separate, high-risk security model. |
| MOBILE-01 | Later | Mobile application | The desktop and web interaction model must be validated first. |
| MULTI-01 | Later | Multi-user deployment | Nexo IA begins as a personal local application. |

## First milestone acceptance criteria

The first usable milestone includes `CHAT-01` through `CHAT-04`. It is complete when:

- a user can create and reopen separate conversations;
- the selected installed Ollama model receives the correct message history;
- response chunks appear progressively and generation can be cancelled;
- messages remain available after restarting the application;
- errors identify whether the application, connection, or model failed;
- automated tests cover conversation isolation and model selection.
