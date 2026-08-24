# Agent capabilities

This document defines Nexo IA's reusable agent concepts and keeps their responsibilities separate.

## Current implemented runtime

Agent mode currently uses Spring AI 2.0.1's `ChatClient` and `ToolSearchToolCallingAdvisor`. It
initially exposes `toolSearchTool`, then progressively reveals only matching callbacks from the
current request's isolated index. `inspect_capabilities` reports that request's exact safe catalog;
action tools include `update_plan`, `remember`, conditional `search_knowledge`, and explicitly
enabled tools from the current user's Docker or personal MCP connections. Agent state, plan
revisions, sanitized tool evidence,
citations, limits, and timing are visible and survive chat navigation. MCP does not yet imply the
full approval Permission Engine, Secret Store, or arbitrary computer control. See
[Spring AI Agent runtime](SPRING_AI_AGENT_RUNTIME.md) and
[MCP runtime and implementation plan](MCP_RUNTIME.md).

## Capability model

```text
User objective
  -> active instructions and selected skills
  -> plan and completion criteria
  -> model decision
  -> Permission Engine
  -> native or MCP tool
  -> evidence and verification
  -> result, artifact, or next plan step
```

## Skills

A skill is a reusable workflow for a recognizable user goal. It teaches the agent how to combine
instructions, references, assets, scripts, and tools consistently.

A skill does not grant permission. It may request native or MCP tools, but every action still passes
through the Permission Engine and the active execution limits.

### Skill package

```text
skill-name/
  SKILL.md           # Metadata, triggers, procedure, boundaries, and expected output
  references/        # Policies, schemas, examples, and detailed guidance
  assets/            # Templates and files used in the output
  scripts/           # Deterministic helpers when instructions are insufficient
  tests/             # Activation, behavior, safety, and regression scenarios
```

### Skill behavior

- Explicit invocation by name.
- Optional implicit activation from a precise description.
- Progressive disclosure: initially expose only compact metadata and load full instructions only
  when selected.
- Declared inputs, outputs, required tools, stopping conditions, and success criteria.
- Version, author, trust level, and compatibility metadata.
- Built-in, organization, team, project, workspace, personal, and session scopes with deterministic
  discovery and precedence.
- Explicit owner, visibility, trust, version, publication state, and declared resource requirements.
- Enable, disable, inspect, import, export, and update controls.
- A preview showing instructions, dependencies, permissions, and scripts before installation.

### Initial built-in skills

- Analyze a project without changing it.
- Diagnose a build or test failure.
- Implement a scoped code change and validate it.
- Review code and report evidence-backed findings.
- Research a topic and produce a cited report.
- Read and summarize a document.
- Build and evaluate a Knowledge Vault.
- Create and test an automation.
- Inspect and troubleshoot an MCP connection.

## Instructions

Instructions are durable rules, conventions, and constraints. They are not task procedures.

Nexo IA should support:

- personal instructions that apply everywhere;
- workspace instructions versioned with a project;
- nested instructions for a specific directory or module;
- temporary session instructions;
- an inspector showing every active source and its precedence;
- size limits and warnings when instructions are truncated or conflict.

More specific instructions override broader instructions. Explicit instructions for the current task
override preferences, but cannot override system security policy.

## Planning

A plan is a visible, editable sequence of steps used when work is multi-stage, ambiguous, or has a
meaningful impact. It is not hidden reasoning and it is not a permission grant.

Nexo IA must create the plan before beginning implementation effects for multi-step, long-running,
high-impact, destructive, or security-sensitive work. Approving the plan approves its strategy, not
the permissions required by its tasks. Each concrete effect still passes through the Permission
Engine.

Each plan contains:

- the objective and definition of done;
- assumptions, constraints, and unresolved questions;
- ordered steps with pending, active, blocked, skipped, or completed status;
- milestones, tasks, subtasks, dependencies, risks, budgets, and permission checkpoints;
- expected evidence or artifact for each step;
- revisions with a reason for the change;
- the final verification and outcome.

The user may approve, edit, reorder, pause, or cancel a plan. Low-risk exploratory work may continue
while a non-blocking question is open; sensitive actions still require the appropriate permission.

The Agent orchestrator executes bounded ready tasks incrementally, persists their structured results,
and provides only relevant context to the next task. It may replan when evidence changes, but any
revision that expands scope, targets, risk, budget, or capabilities requires a new decision. See
[Execution plans](EXECUTION_PLANS.md) for the complete policy.

## Goals

A goal is durable long-running work defined by:

- **Outcome:** the result to produce.
- **Constraints:** boundaries, required tools, compatibility, time, and budget.
- **Verification:** objective evidence that proves completion.

Goals can be paused, resumed, refined, or cancelled. Nexo IA must report blocked work truthfully and
must not mark a goal complete merely because it stopped running.

## Modes

| Mode | Purpose | Default behavior |
|---|---|---|
| Ask | Questions and explanations | Read-only unless the user requests an action. |
| Plan | Explore requirements and produce a reviewable plan | No implementation effects. |
| Agent | Execute a large objective through a visible plan and verified tasks | Plan before effects; permissions remain scoped per action. |
| Build | Implement a scoped change | Edit and validate within an authorized workspace. |
| Review | Inspect work and prioritize findings | Read-only by default. |
| Cowork | Collaborate on a durable objective | Plans, checkpoints, artifacts, and multiple runs. |
| Automation | Execute unattended work | Only pre-authorized capabilities and targets. |

Modes change workflow defaults, not the underlying security boundary.

## Tools, MCP, and plugins

- A **native tool** is a capability implemented and controlled by Nexo IA.
- An **MCP server** provides live external context and actions through a standard protocol.
- A **skill** teaches a reusable workflow that may use multiple tools.
- A **plugin** packages skills, MCP configuration, assets, and metadata for installation.

Installing a plugin does not silently enable its tools or approve their permissions. Nexo IA must show
its contents, origin, required connections, scripts, and requested scopes first.

Sharing a Skill or plugin never shares its author's Knowledge Vaults, Workspaces, memories, secrets,
providers, devices, or permissions. Every resource is resolved for the current authenticated
principal. See [Context isolation and Skill governance](CONTEXT_AND_SKILL_GOVERNANCE.md).

## Computer control

Nexo IA may operate the user's computer on Linux, Windows, and macOS, but it must expose one
platform-neutral capability model to the agent. The model requests an intention such as reading a
file, launching an application, inspecting a process, showing a notification, or interacting with an
approved desktop surface. Deterministic Java code selects and invokes the operating-system adapter.

```text
Agent request
  -> typed computer capability
  -> Permission Engine
  -> operating-system permission check
  -> Linux | Windows | macOS adapter
  -> execution, evidence, and audit event
```

The initial capability families are filesystem, process, application, notification, browser, and
desktop interaction. Support is declared per capability and platform: detecting an operating system
does not imply that every capability is implemented there.

- Every target, argument, working directory, timeout, and environment value is validated outside the
  model.
- Administrator or root elevation is never implicit and cannot be inferred from a broad request.
- Native operating-system consent remains separate from Nexo IA approval.
- Unsupported capabilities fail clearly; they never fall back to improvised model-generated commands.
- Destructive and privacy-sensitive actions require narrower scopes, previews, or fresh confirmation.
- Completion requires post-action evidence rather than trusting an exit code or UI click alone.
- Cross-platform claims require automated contract tests plus real test runs on all three systems.

## Hooks

Hooks are deterministic lifecycle checks around model calls, tool calls, file changes, approvals, or
run completion. They are appropriate for mechanical enforcement such as formatting, secret
detection, policy checks, tests, and notifications.

A hook cannot grant itself broader access. Failure behavior must be explicit: warn, block, retry, or
record only.

## Context inspector

For transparency, every run should expose a safe summary of:

- selected model and generation configuration;
- active instruction sources;
- selected skills and why they activated;
- available and selected tools;
- attached knowledge, memory, and workspace context;
- authenticated principal, organization, Project, context scope, and resource versions;
- plan, permissions, limits, token usage, and stop reason.

The inspector must redact secrets and internal model reasoning.
