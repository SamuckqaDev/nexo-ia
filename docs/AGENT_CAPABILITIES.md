# Agent capabilities

This document defines Nexo IA's reusable agent concepts and keeps their responsibilities separate.

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
- User, workspace, and built-in scopes with deterministic precedence.
- Enable, disable, inspect, import, export, and update controls.
- A preview showing instructions, dependencies, permissions, and scripts before installation.

### Initial built-in skills

- Analyze a project without changing it.
- Diagnose a build or test failure.
- Implement a scoped code change and validate it.
- Review code and report evidence-backed findings.
- Research a topic and produce a cited report.
- Read and summarize a document.
- Build and evaluate a knowledge collection.
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

Each plan contains:

- the objective and definition of done;
- assumptions, constraints, and unresolved questions;
- ordered steps with pending, active, blocked, skipped, or completed status;
- expected evidence or artifact for each step;
- revisions with a reason for the change;
- the final verification and outcome.

The user may approve, edit, reorder, pause, or cancel a plan. Low-risk exploratory work may continue
while a non-blocking question is open; sensitive actions still require the appropriate permission.

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
- plan, permissions, limits, token usage, and stop reason.

The inspector must redact secrets and internal model reasoning.
