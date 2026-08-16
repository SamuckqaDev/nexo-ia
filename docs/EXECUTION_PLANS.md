# Execution plans

## Policy

Nexo IA creates a visible execution plan before starting work that is multi-step, ambiguous,
long-running, high-impact, destructive, security-sensitive, or likely to require several tools.
Simple explanations and low-risk, reversible operations may proceed without a formal plan.

Approving a plan approves the proposed strategy. It does not grant filesystem, process, network,
credential, publication, elevation, or external-service permissions. Concrete effects still pass
through the Permission Engine at the narrowest applicable scope.

## Flow

```text
User objective
  -> context and constraint analysis
  -> execution plan
  -> user review or policy checkpoint
  -> milestone
  -> task
  -> permission decision
  -> execution
  -> evidence and verification
  -> plan update and next ready task
  -> final verification and completion report
```

## Structure

A plan records:

- objective, definition of done, assumptions, constraints, and unresolved questions;
- milestones, tasks, subtasks, dependencies, priority, and current status;
- relevant Workspaces, Knowledge Vaults, files, tools, models, Skills, and capabilities;
- expected effects, risks, permission checkpoints, budgets, and stopping conditions;
- completion criteria and evidence required for every task;
- revisions, their reasons, and their impact on scope and previously completed work.

Task states are `pending`, `ready`, `running`, `awaiting_approval`, `blocked`, `paused`, `completed`,
`failed`, `cancelled`, and `skipped`. Only tasks whose dependencies and required approvals are
satisfied become ready.

## Incremental execution

The orchestrator executes one bounded task at a time by default. Each task receives the overall goal
summary, its local objective, relevant decisions and context, dependency results, limits, and
completion criteria—not the complete accumulated history. Its structured result and evidence are
persisted for downstream tasks.

A large task may be decomposed further. Independent tasks may run concurrently only when their
effects cannot conflict and the plan, resource limits, and permission policy allow it explicitly.

## Replanning

Nexo IA may split, add, reorder, retry, skip, or remove tasks when evidence changes the plan. Every
revision is visible and auditable. A revision that expands the objective, targets, risk, budget, or
required capabilities pauses affected execution for a new user decision. Replanning never expands
permissions automatically.

## Persistence and completion

Goals, plans, tasks, revisions, approvals, results, and evidence are persisted so work can pause,
resume, survive application restarts, and avoid duplicating completed effects. A task is complete
only when its declared evidence passes verification. A goal is complete only when the definition of
done and final verification are satisfied; otherwise Nexo IA reports remaining or blocked work.
