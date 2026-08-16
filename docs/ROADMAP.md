# Learning and development roadmap

## Release 0.1 checkpoint

Release `0.1` deliberately combines the minimum outcomes of Phases 0, 1, and 2: Owner bootstrap,
default organization, a second managed user, local Ollama provider and model discovery, isolated
persistent conversations, SSE streaming and cancellation, token and latency attribution, audit, and
administration UI. It does not pull later-phase capabilities forward. See
[MVP and release strategy](MVP_AND_RELEASE_STRATEGY.md).

## Phase 0 — Foundations and identity

- Define the purpose, principles, scope, and identity.
- Select the technology stack using documented criteria.
- Prepare the repository, conventions, and testing strategy.
- Model organizations, users, teams, resource ownership, access grants, auditable sessions, password
  security, and installation bootstrap for the Nexo Owner.
- Establish organization-aware data isolation and negative authorization tests before shared data.

## Phase 1 — First contact with the LLM

- Learn the Ollama API.
- Send a message and interpret its response.
- Study tokens, messages, context, and generation parameters.
- Implement a minimal chat with streaming.
- Establish the Provider Registry for local models and record model, provider, latency, and token
  usage for every request.

## Phase 2 — Persistent conversations

- Model conversations and messages.
- Persist history.
- Assemble context within a token budget.
- Test isolation between conversations.
- Bind each request and cache entry to its authenticated principal, organization, conversation, and
  provider policy.
- Satisfy every release `0.1` security, quality, operational, and acceptance gate before beginning the
  Knowledge release.

## Phase 3 — RAG

- Create local Knowledge Vaults with explicit roots, policies, and portable source files.
- Ingest structure-aware notes and documents, including links, backlinks, tags, and properties.
- Generate embeddings and combine semantic, textual, metadata, and relationship-aware retrieval.
- Display original sources and relationship provenance used in responses.
- Create a question set to evaluate retrieval and answers.
- Enforce user, organization, Project, Vault, source, and sensitivity filters before retrieval results
  reach the model.

## Phase 4 — Tools and permissions

- Define the tool contract.
- Implement read-only tools.
- Classify risks and request approval.
- Add scoped, audited write and execution capabilities.
- Introduce Privacy Gateway classification, minimization, secret detection, redaction, and explicit
  remote-provider transmission policy.

## Phase 5 — Cross-platform computer control

- Define the platform-neutral computer capability contract.
- Implement and test Linux, Windows, and macOS adapters incrementally.
- Add capability discovery, application launching, process inspection, and notifications.
- Add desktop and browser interaction only after platform permissions and isolation are explicit.
- Separate Nexo Server orchestration from paired Nexo Companion endpoint execution.
- Add device identity, inventory, access policy, heartbeat, revocation, and execution snapshots.

## Phase 6 — MCP

- Study the protocol, transports, and lifecycle.
- Connect a simple MCP server.
- Discover tools progressively.
- Apply the same permission rules to MCP tools.
- Add the MCP Hub with provenance, license, locality, data-exposure, health, scope, usage, and cost
  inspection.
- Build the Nexo MCP toolkit and validate one free, maintained tool integration end to end.

## Phase 7 — Agent

- Implement the think, act, observe, and respond loop.
- Require a visible plan before multi-step or consequential execution.
- Decompose Goals into milestones, tasks, subtasks, dependencies, and completion criteria.
- Execute bounded ready tasks incrementally and persist results for downstream work.
- Separate plan approval from per-action Permission Engine decisions.
- Support visible replanning without implicit scope or permission expansion.
- Limit rounds, time, tools, and context volume.
- Detect repetition and stop unproductive loops.
- Verify results before reporting completion.

## Phase 8 — Skills, instructions, and planning

- Define reusable skills with progressive instruction loading.
- Add explicit invocation, safe implicit activation, and skill tests.
- Add Skill ownership, trust, versioning, and built-in, organization, team, Project, Workspace,
  personal, and session scopes.
- Separate Skill discovery, viewing, editing, testing, publishing, installing, enabling, executing,
  and deletion rights.
- Reauthorize every Skill dependency for the current principal; sharing a Skill never shares its
  creator's Vaults, Workspaces, memory, secrets, providers, devices, or permissions.
- Layer organization, Project, personal, Workspace, directory, Skill, session, and task instructions
  predictably without weakening security policy.
- Add Plan, Goal, Build, Ask, and Review workflows without changing security boundaries.
- Expose a context inspector without revealing secrets or private model reasoning.

## Phase 9 — Memory and reliability

- Separate history, scoped Memory, and retrieved Vault knowledge.
- Add session, personal, Project, team, and organization Memory with provenance, approval, expiration,
  inspection, rescoping, and deletion propagation.
- Share relevant personal Memory across only its owner's authorized chats.
- Persist immutable context envelopes and test cross-user, cross-organization, retrieval, and cache
  isolation as release-blocking scenarios.
- Add tracing and metrics.
- Add token, latency, cost, and resource attribution with budgets and quotas per run, user, team,
  Project, model, and organization.
- Recover interrupted executions only when necessary.
- Create security, quality, and performance evaluations.

## Phase 10 — Cowork

- Create durable sessions around explicit objectives.
- Attach Workspaces, Knowledge Vaults, plans, runs, and artifacts to a session.
- Add human checkpoints and resumable task states.
- Support team participation, ownership transfer, scoped sharing, activity attribution, and a shared
  approval queue without exposing personal context.
- Produce evidence-backed completion reports.
- Add templates only after real workflows reveal reusable patterns.

## Phase 11 — Scheduled tasks and automations

- Persist one-time and recurring schedules with explicit timezones.
- Define pre-authorized capabilities, targets, budgets, and failure policies.
- Run every occurrence as an explicit user or service account with a frozen authorized context
  envelope rather than the last interactive user's session.
- Add manual, assisted, supervised, pre-authorized, and unattended autonomy policies with pause,
  resume, cancellation, revocation, anomaly limits, and emergency stop.
- Execute isolated, idempotent runs without an active user session.
- Add dry runs, manual tests, history, notifications, and human-review states.
- Add month, week, day, and agenda views over automation occurrences, Cowork milestones, checkpoints,
  and approval deadlines.
- Add filters, event details, conflict warnings, schedule management, and run actions.
- Add event triggers and chained workflows only after scheduled runs are safe and reliable.

## Phase 12 — Image generation

- Define a provider-independent image request and result contract.
- Integrate a local ComfyUI workflow through asynchronous, cancellable jobs.
- Preserve prompts, models, parameters, files, and provenance.
- Attach generated images to Chat, Cowork, Skills, and Automation results.
- Evaluate remote Spring AI image providers only through explicit opt-in.

## Phase 13 — Product discovery, UX, and prototyping

- Turn product ideas into requirements, personas, journeys, stories, acceptance criteria, MVPs, and
  roadmaps through reviewable Skills.
- Create wireframes, navigation flows, design tokens, component specifications, and accessible,
  responsive prototypes.
- Build runnable HTML, CSS, and React prototypes and compare implementation screenshots with approved
  design artifacts.
- Keep generated design assets, references, decisions, and provenance in authorized Projects and
  Knowledge Vaults.

## Phase 14 — Software and documentation engineering

- Analyze architecture, source, dependencies, builds, tests, logs, and changes within authorized
  Workspaces.
- Implement, refactor, review, test, and document scoped changes through plans, diffs, evidence, and
  recovery controls.
- Generate and maintain READMEs, API documentation, ADRs, guides, changelogs, release notes, diagrams,
  and bilingual documentation from traceable sources.
- Detect code-documentation drift and schedule governed maintenance through Cowork and Automations.
- Add Project database connections, Database Explorer, schema documentation, and granular metadata,
  query, mutation, migration, backup, restore, and administration capabilities.
- Build the Database Safety Engine with statement parsing, impact preview, row and time limits,
  environment policy, recovery checkpoints, transactional execution where supported, validation,
  rollback, privacy controls, and audit.
- Validate PostgreSQL first with real destructive-failure and recovery tests before adding other
  database adapters or MCP integrations.

## Phase 15 — Authorized security validation

- Add threat modeling, static analysis, dependency and secret scanning, configuration review, and
  controlled web or API testing through free-first native or MCP integrations.
- Require a recorded owner, authorized target, time window, techniques, network limits, intensity,
  evidence policy, and emergency stop before active security testing.
- Produce reproducible findings, severity, remediation, protected evidence, and verified retests.
- Deny target expansion, destructive exploitation, credential reuse, and unapproved external testing.

## Phase 16 — Delivery and operations

- Prepare governed CI/CD, packaging, releases, backups, retention, health, metrics, and incident
  workflows without granting implicit publication or production access.
- Add explicit deployment approvals, environment policies, secret references, rollback evidence, and
  separation between build, publish, and deploy permissions.
- Operate local, central-server, and DGX deployments with device, provider, token, cost, capacity, and
  audit dashboards.
- Introduce distributed infrastructure only after measured scale or availability requirements prove
  the modular deployment insufficient.

## Release gates across all phases

- No feature is complete without authorization, isolation, privacy, audit, accessibility, failure,
  cancellation, recovery, and evidence tests appropriate to its risk.
- Linux, Windows, and macOS claims require real platform verification; team claims require negative
  cross-user and cross-organization tests.
- Remote-provider features disclose transmitted context and measured usage; autonomous features have
  budgets, stopping conditions, revocation, and emergency controls.
- Documentation, requirement identifiers, decisions, implementation, and tests remain traceable and
  are updated together.
- Database mutation claims require database-version-specific transaction, locking, backup, restore,
  rollback, redaction, and failure-injection evidence.
