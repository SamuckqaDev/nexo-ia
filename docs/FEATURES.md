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

## Knowledge Vaults and RAG

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| RAG-01 | P1 | Knowledge Vaults | Create independently searchable local Vaults with a purpose, authorized roots, and retrieval policy. |
| RAG-02 | P1 | Vault ingestion | Parse, chunk, embed, and index supported Vault content with visible status. |
| RAG-03 | P1 | Semantic retrieval | Retrieve relevant passages with scores and metadata. |
| RAG-04 | P1 | Grounded answers | Cite supporting passages and admit when evidence is insufficient. |
| RAG-05 | P1 | Evaluation set | Measure retrieval and answer quality with repeatable questions. |
| RAG-06 | P2 | Incremental updates | Re-index only documents whose content or configuration changed. |
| RAG-07 | P1 | Interlinked knowledge | Parse Markdown links, wikilinks, backlinks, tags, and frontmatter as explicit relationships. |
| RAG-08 | P1 | Relationship-aware retrieval | Combine textual, vector, metadata, and Vault relationship signals when assembling context. |
| RAG-09 | P1 | Source provenance | Preserve Vault, file, section, version, authority, and relationship paths in citations. |
| RAG-10 | P1 | Rebuildable index | Recreate PostgreSQL metadata and embeddings from human-readable Vault sources. |
| RAG-11 | P1 | Vault permissions | Authorize reading, indexing, creating, editing, moving, and deleting independently. |
| RAG-12 | P2 | Obsidian compatibility | Support common Obsidian Markdown, wikilinks, frontmatter, tags, attachments, and later Canvas integration. |
| RAG-13 | P1 | Vault Explorer | Show personal, shared, team, organization, and archived Vaults in the frontend. |
| RAG-14 | P1 | Vault detail | Display sources, files, graph, tags, relationships, citations, indexing, permissions, activity, and settings. |
| RAG-15 | P1 | Vault operations | Search, open sources, reindex, manage metadata and relationships, share, export, archive, and delete according to permission. |

## Workspaces and native tools

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| WORK-01 | P1 | Workspace authorization | Grant access only to an explicit directory and preserve its canonical path. |
| TOOL-01 | P1 | Tool contract | Validate structured inputs and return structured results and errors. |
| TOOL-02 | P1 | Read-only tools | List, read, and search authorized workspace content. |
| TOOL-03 | P1 | File changes | Propose changes, request permission, apply them, and present a diff. |
| TOOL-04 | P1 | Controlled commands | Execute approved commands with workspace, timeout, and output limits. |
| TOOL-05 | P2 | Change recovery | Record enough information to review and recover agent file changes. |

## Project database access

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| DB-01 | P1 | Project connections | Register database connections by organization, Project, environment, owner, and secret reference. |
| DB-02 | P1 | JDBC adapters | Support tested relational adapters beginning with PostgreSQL without coupling agent contracts to one database. |
| DB-03 | P1 | Database Explorer | Show authorized schemas, tables, views, columns, keys, constraints, relationships, indexes, and migrations. |
| DB-04 | P1 | Granular capabilities | Separate metadata, select, insert, update, delete, procedure, schema, migration, backup, restore, and administration grants. |
| DB-05 | P1 | Environment policy | Apply distinct development, test, staging, and production approvals, windows, limits, and recovery requirements. |
| DB-06 | P1 | Statement analysis | Parse, classify, normalize, and policy-check statements instead of trusting model descriptions. |
| DB-07 | P1 | Mutation preview | Estimate affected rows and show a bounded, redacted preview before material update or deletion. |
| DB-08 | P1 | Mutation limits | Deny unbounded predicates, destructive commands, excessive rows, duration, locks, or resource use by default. |
| DB-09 | P1 | Transactional execution | Use a transaction and rollback failed validation when the database and operation support it safely. |
| DB-10 | P1 | Recovery checkpoint | Require and verify an appropriate row backup, dump, snapshot, restore point, inverse migration, or forward-recovery plan. |
| DB-11 | P1 | Governed migrations | Prefer versioned Project migrations and separate preparation from execution permission per environment. |
| DB-12 | P1 | Post-change validation | Verify affected rows, constraints, invariants, schema, application tests, and requested outcome before completion. |
| DB-13 | P1 | Data privacy | Apply row, column, personal-data, export, provider, and retention policy to queries, previews, logs, and model context. |
| DB-14 | P1 | Database audit | Link statements and migrations to requester, approver, plan, recovery, effect, validation, model, provider, location, and evidence. |
| DB-15 | P2 | Database MCP adapters | Add governed MCP integrations for independent or non-JDBC databases without weakening safety policy. |

## Permission Engine

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| PERM-01 | P1 | Risk classification | Classify each capability by effect, target, and reversibility. |
| PERM-02 | P1 | Policy evaluation | Combine tool risk, requested scope, and user rules deterministically. |
| PERM-03 | P1 | Approval flow | Show the exact action, target, reason, and risk before approval. |
| PERM-04 | P1 | Scoped grants | Limit approval to one action, run, workspace, or explicitly chosen scope. |
| PERM-05 | P1 | Audit trail | Record requests, decisions, execution results, and ownership. |
| PERM-06 | P2 | Custom policies | Allow users to create stricter local policies with validation and previews. |

## Cross-platform computer control

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| OS-01 | P1 | Platform detection | Detect the operating system, architecture, environment, and available capabilities without guessing. |
| OS-02 | P1 | Capability contract | Represent files, processes, applications, terminal, notifications, and desktop actions with platform-neutral requests. |
| OS-03 | P1 | Linux adapter | Implement supported operations for Linux desktops, shells, files, processes, and applications. |
| OS-04 | P1 | Windows adapter | Implement supported operations for Windows, PowerShell, files, processes, and applications. |
| OS-05 | P1 | macOS adapter | Implement supported operations for macOS, shells, files, processes, applications, and approved automation APIs. |
| OS-06 | P1 | Capability discovery | Report which operations are available, unavailable, restricted, or require setup on the current machine. |
| OS-07 | P1 | Command abstraction | Select an explicit platform implementation instead of translating commands through model improvisation. |
| OS-08 | P1 | Application launching | Open approved applications, files, folders, and URLs with exact targets and visible results. |
| OS-09 | P1 | Process control | Inspect and control authorized processes with PID, ownership, timeout, and effect validation. |
| OS-10 | P1 | System notifications | Send local progress, approval, completion, and failure notifications through the platform adapter. |
| OS-11 | P2 | Desktop interaction | Use accessibility or UI-automation APIs only after explicit OS permission and application scope. |
| OS-12 | P2 | Browser interaction | Control an isolated or explicitly authorized browser session with observable navigation and actions. |
| OS-13 | P2 | Environment setup | Diagnose and propose platform-specific dependency setup without silently changing the host. |
| OS-14 | P2 | Cross-platform test matrix | Verify common contracts and platform-specific behavior on real or virtualized Linux, Windows, and macOS environments. |

### Computer-control safety rules

- The model selects a capability; application code selects the platform implementation.
- Every effect carries an exact target, scope, reason, risk, timeout, and expected verification.
- Shell access, desktop access, accessibility access, and administrator access are separate grants.
- Administrator elevation is never implied by approving a plan, Cowork session, or automation.
- Destructive, credential, security-setting, persistence, package-installation, and external-message
  actions require dedicated policies.
- The adapter passes the minimum environment and never exposes the full process environment to the
  model or command by default.
- Platform differences produce explicit unsupported results instead of guessed command substitution.
- Automation runs use the same rules and may not wait indefinitely for an interactive OS prompt.

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
| AGENT-07 | P1 | Hierarchical decomposition | Divide a Goal into milestones, tasks, and subtasks with explicit dependencies. |
| AGENT-08 | P1 | Incremental context | Give each task only the goal summary, relevant context, dependency results, limits, and criteria it needs. |
| AGENT-09 | P1 | Persistent task state | Persist plans, task states, revisions, approvals, results, and evidence across restarts. |
| AGENT-10 | P1 | Controlled replanning | Split, add, reorder, retry, skip, or remove tasks visibly without expanding permissions automatically. |
| AGENT-11 | P1 | Readiness evaluation | Execute only tasks whose dependencies, context, resources, and approvals are satisfied. |
| AGENT-12 | P2 | Safe parallel tasks | Run independent tasks concurrently only when effects, limits, and policy cannot conflict. |

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
| SKILL-09 | P1 | Skill ownership | Record creator, owner, origin, version, trust, visibility, and publication state. |
| SKILL-10 | P1 | Skill scopes | Support built-in, organization, team, project, workspace, personal, and session Skills. |
| SKILL-11 | P1 | Governed sharing | Separate viewing, editing, testing, publishing, installing, enabling, executing, and deletion rights. |
| SKILL-12 | P1 | Resource reauthorization | Resolve every declared Vault, Workspace, secret, provider, device, tool, and permission for the current user and run. |
| SKILL-13 | P1 | Activation provenance | Record explicit invocation or the reason, scope, and policy behind implicit activation. |
| INST-01 | P1 | Instruction scopes | Load personal, workspace, directory, and session instructions predictably. |
| INST-02 | P1 | Instruction inspector | Show active sources, precedence, conflicts, and truncation warnings. |
| PLAN-01 | P1 | Planning mode | Research and create a reviewable plan without implementation effects. |
| PLAN-02 | P1 | Plan editor | Approve, edit, reorder, pause, cancel, and explain revisions to plan steps. |
| PLAN-03 | P1 | Plan-before-action policy | Require a visible plan before multi-step, long-running, high-impact, destructive, or security-sensitive effects. |
| PLAN-04 | P1 | Strategy and permission separation | Treat plan approval separately from capability grants for concrete actions. |
| PLAN-05 | P1 | Task completion contract | Define expected evidence and objective completion criteria for each task. |
| GOAL-01 | P1 | Durable goals | Track outcome, constraints, verification, status, and remaining work. |
| GOAL-02 | P1 | Goal steering | Pause, resume, refine, cancel, or add context without losing progress. |
| MODE-01 | P1 | Interaction modes | Provide Ask, Plan, Agent, Build, Review, Cowork, and Automation workflows. |
| HOOK-01 | P2 | Lifecycle hooks | Run deterministic checks at declared lifecycle points with explicit failure behavior. |
| CTX-01 | P1 | Context inspector | Explain active model, instructions, skills, tools, knowledge, limits, and stop reason. |
| CTX-02 | P1 | Context envelope | Bind each model request to one principal, organization, Project, objective, run, provider policy, and versioned resource set. |
| CTX-03 | P1 | Context isolation | Prevent cross-user, cross-organization, cross-project, and cross-run leakage in retrieval, prompts, results, and caches. |
| CTX-04 | P1 | Context minimization | Select only relevant authorized content within token, sensitivity, provider, and retention budgets. |
| CTX-05 | P1 | Context provenance | Record selected sources, versions, reasons, redactions, truncation, Skills, memories, Vault passages, and policy decisions. |

## Cowork

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| COWORK-01 | P1 | Cowork sessions | Create a durable work session with a title, objective, status, and owner. |
| COWORK-02 | P1 | Context selection | Attach authorized Workspaces, Knowledge Vaults, and relevant conversations. |
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

## Calendar and task overview

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| CAL-01 | P1 | Calendar views | Display month, week, day, and chronological agenda views. |
| CAL-02 | P1 | Unified schedule | Show one-time tasks, recurring automations, Cowork milestones, checkpoints, and approval deadlines together. |
| CAL-03 | P1 | Execution status | Distinguish scheduled, running, completed, failed, blocked, paused, skipped, overdue, and awaiting-approval items. |
| CAL-04 | P1 | Filters | Filter by project, Cowork session, automation, Skill, status, and priority. |
| CAL-05 | P1 | Event details | Explain the objective, next occurrence, timezone, authorized capabilities, targets, limits, and previous runs. |
| CAL-06 | P1 | Schedule management | Create, edit, reschedule, pause, resume, and cancel eligible items from the calendar. |
| CAL-07 | P1 | Run actions | Offer dry run, run now, inspect history, and open the related Cowork session or automation. |
| CAL-08 | P1 | Conflict awareness | Warn about overlapping runs, unavailable resources, missed occurrences, and approval deadlines. |
| CAL-09 | P1 | Notification controls | Configure reminders and notifications before execution and after completion, failure, or blockage. |
| CAL-10 | P2 | Drag rescheduling | Reschedule eligible items by drag and drop while preserving timezone and recurrence semantics. |
| CAL-11 | P2 | External calendars | Integrate selected external calendars through reviewed MCP connections without granting execution permission implicitly. |
| CAL-12 | P2 | Calendar export | Export authorized schedule information without exposing secrets, private prompts, or unnecessary execution details. |

The calendar is a presentation and management layer over persisted Cowork tasks, automation
definitions, and scheduled occurrences. It is not a second scheduler and must not maintain an
independent source of truth.

### Calendar safety rules

- Moving an event changes its schedule, not its execution permissions or authorized targets.
- A calendar import creates proposed events until the user reviews their Nexo IA behavior.
- External calendar access never grants a remote provider access to local workspaces or tools.
- Sensitive objectives and targets are hidden from lock-screen and external notifications by default.
- Every schedule change produces an audit event and recalculates future occurrences deterministically.

## Memory, transparency, and operations

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| MEM-01 | P1 | Explicit memory | Save a fact only through an observable user or agent decision. |
| MEM-02 | P1 | Memory controls | Inspect, edit, disable, and delete stored memories. |
| MEM-03 | P1 | Memory scopes | Support session, personal, project, team, and organization memories with explicit visibility. |
| MEM-04 | P1 | Cross-chat personal memory | Select relevant personal memories across their owner's chats without exposing other users' context. |
| MEM-05 | P1 | Shared memory governance | Require explicit promotion, provenance, permission, and scope for team or organization memory. |
| MEM-06 | P1 | Memory provenance | Record source, creator or approver, confidence, sensitivity, creation, use, revision, and expiration. |
| MEM-07 | P1 | Memory usage inspector | Show which memories influenced a run without exposing unrelated or unauthorized memories. |
| MEM-08 | P1 | Memory deletion propagation | Remove deleted memories from derived indexes and caches according to retention policy. |
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

## Identity, organizations, and access

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| IAM-01 | P1 | Installation bootstrap | Create the first Nexo Owner without coupling it to an operating-system root account. |
| IAM-02 | P1 | Authentication | Provide secure login, logout, revocable sessions, password reset, and progressive login throttling. |
| IAM-03 | P1 | Users and teams | Create, invite, suspend, restore, and organize members and service accounts. |
| IAM-04 | P1 | Authorization | Combine roles, ownership, explicit grants, capability policy, and data policy. |
| IAM-05 | P1 | Resource isolation | Isolate personal conversations, memories, Vaults, Workspaces, credentials, and runs by default. |
| IAM-06 | P1 | Sharing | Share selected resources with users or teams using explicit read, contribute, execute, and manage rights. |
| IAM-07 | P2 | MFA and federation | Add MFA and external OIDC login without changing ownership semantics. |

## Providers, privacy, usage, and autonomy

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| PROV-01 | P1 | Provider registry | Configure local, organization-hosted, and remote model providers with scoped availability. |
| PROV-02 | P1 | Secret references | Store provider credentials outside prompts, logs, and ordinary configuration records. |
| PROV-03 | P1 | First-use provider setup | Require a user with no provider to choose a provider and pass a connection test before use. |
| PROV-04 | P1 | User provider configuration | Save each user's provider endpoint and protected configuration with ownership isolation. |
| PROV-05 | P1 | Model discovery | Synchronize models from the provider, allow refresh, and expose only discovered models for selection. |
| PROV-06 | P1 | Provider lifecycle | Allow users to edit, retest, select a default model, disable, and remove their own providers. |
| PRIV-01 | P1 | Privacy Gateway | Classify and minimize context, detect secrets and personal data, redact, and enforce provider transmission policy. |
| PRIV-02 | P1 | Remote transparency | Explain which selected data and provider leave the trusted boundary; never use silent remote fallback. |
| USAGE-01 | P1 | Token accounting | Attribute input, output, cached when available, model, provider, latency, and estimated cost to users, teams, projects, and runs. |
| USAGE-02 | P1 | Budgets and quotas | Enforce per-request, run, user, team, project, model, and organization limits and alerts. |
| AUTOLEVEL-01 | P1 | Autonomy policy | Support manual, assisted, supervised, pre-authorized, and unattended levels without bypassing plans or permissions. |
| AUTOLEVEL-02 | P1 | Operational controls | Pause, resume, cancel, revoke, and emergency-stop autonomous work. |

## Devices and Nexo Companion

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| DEV-01 | P1 | Device pairing | Pair a Companion through user confirmation and unique, rotatable, revocable credentials. |
| DEV-02 | P1 | Cross-platform Companion | Execute authorized capabilities on paired Linux, Windows, and macOS endpoints. |
| DEV-03 | P1 | Device inventory | Record owner, type, hostname, OS, architecture, CPU, memory, GPU, storage, network observation, Companion version, and capabilities according to policy. |
| DEV-04 | P1 | Inventory history | Preserve the latest state, material changes, and timestamped execution snapshots. |
| DEV-05 | P1 | Device access policy | Control visibility, execution, approval, administration, Workspaces, capabilities, hours, budgets, and autonomy per device. |
| DEV-06 | P1 | Execution location | Distinguish server, device, sandbox, MCP, and remote-provider processing and execution locations. |
| DEV-07 | P1 | Local approval | Require target-device confirmation for policy-selected sensitive actions. |
| DEV-08 | P1 | Device audit | Link requester, plan, model, provider, permission, device snapshot, effect, evidence, usage, and result. |
| DEV-09 | P1 | Health and revocation | Track heartbeat and health, block new work after revocation, and cancel eligible active work. |

## MCP ecosystem

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| MCPHUB-01 | P1 | MCP Hub | Catalog, configure, inspect, health-check, scope, disable, and audit MCP servers. |
| MCPHUB-02 | P1 | Tool provenance | Display source, license, version, locality, data exposure, permissions, limits, and cost. |
| MCPHUB-03 | P2 | Nexo MCP toolkit | Provide typed contracts, validation, risk metadata, errors, audit, timeouts, cancellation, and tests for project-owned servers. |
| MCPHUB-04 | P1 | Free-first selection | Prefer maintained local and open-source tools without treating MCP as a way to bypass licensing or fees. |

## Product, UX, UI, and prototyping

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| PROD-01 | P2 | Product discovery | Convert an idea into problem, audience, value, assumptions, requirements, MVP, backlog, and roadmap artifacts. |
| PROD-02 | P2 | User journeys | Create personas, journeys, stories, flows, acceptance criteria, and testable outcomes with traceable revisions. |
| DESIGN-01 | P2 | UX flows and wireframes | Produce reviewable navigation flows and wireframes before implementation effects. |
| DESIGN-02 | P2 | Design systems | Define colors, typography, spacing, tokens, components, states, responsiveness, and accessibility rules. |
| DESIGN-03 | P2 | Runnable prototypes | Build authorized HTML, CSS, and React prototypes from approved product and design artifacts. |
| DESIGN-04 | P2 | Visual validation | Compare screenshots, responsive states, accessibility evidence, and implementation against approved designs. |
| BRAND-01 | P2 | Brand identity | Create names, concepts, logos, SVG variants, palettes, icons, mockups, and governed provenance. |

## Software and documentation engineering

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| ENG-01 | P1 | Project analysis | Explain architecture, dependencies, source, configuration, build, tests, and risks without changing the target. |
| ENG-02 | P1 | Scoped implementation | Plan, create, modify, refactor, and review code through authorized Workspaces and visible diffs. |
| ENG-03 | P1 | Verification | Run builds, formatters, linters, tests, and evidence checks before reporting completion. |
| ENG-04 | P1 | Git workflow | Separate inspection, branch, commit, remote push, pull request, release, and publication permissions. |
| DOC-01 | P1 | Technical documentation | Create and maintain READMEs, API docs, ADRs, guides, changelogs, release notes, diagrams, and tutorials. |
| DOC-02 | P1 | Documentation traceability | Relate requirements, decisions, code, tests, versions, and sources and detect stale documentation. |
| DOC-03 | P2 | Formats and language | Support governed Markdown, HTML, PDF, diagrams, and bilingual documentation workflows. |

## Authorized security validation

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| SEC-01 | P2 | Security scope | Require asset ownership or authorization, exact targets, time window, techniques, limits, contacts, and stop conditions. |
| SEC-02 | P2 | Passive analysis | Perform threat modeling, static analysis, dependency, secret, and configuration scanning with free-first tools. |
| SEC-03 | P2 | Controlled active testing | Run approved web, API, authentication, authorization, and network checks within recorded boundaries. |
| SEC-04 | P2 | Protected evidence | Store reproducible evidence with restricted access, redaction, retention, and complete provenance. |
| SEC-05 | P2 | Findings and retest | Report severity, impact, remediation, confidence, evidence, and verified retest outcome. |
| SEC-06 | P2 | Security stop controls | Enforce intensity, concurrency, rate, network, credential, destructive-action, and emergency-stop policy. |

## Delivery and operations

| ID | Priority | Feature | Expected behavior |
|---|---|---|---|
| OPS-01 | P2 | Governed delivery | Separate build, package, publish, deploy, migrate, and rollback capabilities and approvals. |
| OPS-02 | P2 | Environment policy | Bind secrets, providers, devices, targets, budgets, and allowed operations to each environment. |
| OPS-03 | P2 | Observability | Monitor health, logs, metrics, usage, cost, capacity, runs, devices, and provider reliability. |
| OPS-04 | P2 | Backup and retention | Define encrypted backup, restore verification, retention, deletion, and disaster-recovery behavior. |
| OPS-05 | P2 | Incident workflows | Coordinate detection, containment, evidence, communication, recovery, and post-incident actions through Cowork. |

## Intentionally postponed

| ID | Priority | Feature | Reason |
|---|---|---|---|
| VIDEO-01 | Later | Video generation | Adds substantial model, storage, and job complexity after images. |
| VOICE-01 | Later | Speech input and output | Adds runtime and interface complexity before the core is stable. |
| MOBILE-01 | Later | Mobile application | The desktop and web interaction model must be validated first. |

## First milestone acceptance criteria

Release `0.1` combines the P0 conversation path with the minimum identity, organization, provider,
usage, audit, security, interface, and operational capabilities needed to make it a real multi-user
product slice. Its frozen scope, exclusions, domain records, release gates, and seven acceptance
journeys are defined in [MVP and release strategy](MVP_AND_RELEASE_STRATEGY.md).

No P1 or P2 label alone adds a feature to release `0.1`; the release document is authoritative.
