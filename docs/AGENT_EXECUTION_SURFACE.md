# Agent execution surface

> How Nexo shows agent work: not as noise inside the chat bubble, but as a reviewable **side panel of
> plans and confirmed tasks**, following the plan/activity separation used by Google Jules and
> the artifact-first verification direction of Google Antigravity. The chat stays a
> conversation; the *work* becomes inspectable deliverables. This realizes the identity promise —
> **"Your knowledge. Your tools. Your control"** — by making every governed step visible and auditable
> without drowning the reply.

Companion to [Permission profiles and unlock levels](PERMISSION_PROFILES.md),
[Spring AI Agent runtime](SPRING_AI_AGENT_RUNTIME.md), and [MCP runtime](MCP_RUNTIME.md).

## 1. The problem with in-bubble execution

Plan revisions and tool activity used to render inside the assistant message. As soon as an Agent run
made several tool calls, the bubble became a scroll of low-level steps and the actual answer was
buried. The runtime also lacked a strong visual distinction between a proposed action and a confirmed
one. Nexo now keeps the answer readable and exposes the execution record in the conversation side
panel.

## 2. Two surfaces, one run

| Surface | Holds | Purpose |
|---|---|---|
| **Chat bubble** | the final answer + a compact **status chip** (running / needs approval / done / failed, with elapsed time and a "view Tasks" affordance) | stays a readable conversation |
| **Plan panel** | the latest revision, ordered step titles, observable descriptions, status, and progress | what Nexo intends to do |
| **Tasks panel** | the run state plus plan publication, real tool executions, and media jobs with status, time, duration, and evidence | what the runtime actually did |

The panel is the existing conversation workspace, promoted into separate **Plan** and **Tasks**
surfaces. It follows live events while connected and restores from persisted state
after navigation or reload — the browser SSE connection is an **observer, not the execution owner**
(leaving the chat never cancels the server run).

## 3. Artifacts (Antigravity-style deliverables)

Each Agent run produces reviewable artifacts, streamed into the panel as they materialize:

| Artifact | Nexo source | When |
|---|---|---|
| **Task list / Plan** | `update_plan` revisions (≤12 steps, one `IN_PROGRESS`), with title and observable description | published at run start, revised live |
| **Tasks** | persisted tool calls and media jobs, each with a safe summary, status, timestamp, duration, and evidence | during execution |
| **Evidence** | sanitized tool evidence — citations from `search_knowledge`, saved entries from `save_to_vault`, `mcp_*` results | as each tool completes |
| **Approvals** | `permission_required` requests with the family, target, and reason; Approve / Deny inline | when a `REQUIRES_APPROVAL` capability is reached |
| **Walkthrough** | a closing summary: what was done, evidence produced, and how to verify | at terminal state |

A tool call never appears as raw JSON or chain-of-thought; it appears as a governed step with a safe
summary and a status. Ownership ids, endpoints, secrets, embeddings, and raw arguments never reach the
panel (only an argument digest), matching the sanitized-evidence contract.

## 4. Event routing

The surface needs **no new execution engine** — it re-routes the typed SSE events the Spring AI
advisor loop already emits. Only the *destination* changes: activity leaves the bubble.

```text
Spring AI advisor loop (ToolCallingAdvisor / ToolSearchToolCallingAdvisor)
  token / thinking      -> chat bubble (answer + transient reasoning)
  plan_updated          -> panel: Plan + plan publication in Tasks
  tool_started          -> panel: open a confirmed Task row
  tool_completed        -> panel: close the Task row with status + evidence
  permission_required   -> panel: approval card (blocks that capability until answered)
  agent_state           -> chat status chip + panel header (QUEUED..COMPLETED/FAILED/CANCELLED)
  usage / completed     -> chat bubble: final answer + walkthrough artifact
```

New event to add for the permission engine: **`permission_required`** (family, target label, reason,
execution id). Its Approve/Deny reply resumes or denies exactly one gated tool call — per-action,
per-session, never generalized (see [Permission profiles](PERMISSION_PROFILES.md) §8).

## 5. Backend contract (Spring AI 2.0)

Implemented now:

- The deterministic fallback decomposes the actual objective into titled steps with concise,
  observable descriptions. The model may revise that complete plan through `update_plan`.
- Knowledge search, personal-memory writes, and external MCP research requested explicitly are
  evidence-gated. Provider prose is buffered until the required callback returns successful evidence;
  a missing, ignored, denied, unavailable, or failed tool can never be persisted as a successful
  action.
- `inspect_capabilities` is recorded as a real Task event rather than remaining invisible
  narration. All visible tool events are correlated, sanitized, persisted, and restorable.
- Required evidence also governs fallback-plan completion: a step that requires `remember`,
  `search_knowledge`, or `mcp_*` stays pending unless a matching execution actually succeeded.

Still deferred:

- **Approval barrier in the governed callback.** A `REQUIRES_APPROVAL` `ToolCallback` suspends on first
  invocation, emits `permission_required`, and blocks the request-scoped executor thread (virtual
  thread — cheap to park) until the user's decision arrives or the run is cancelled/times out. On
  approve it proceeds; on deny it returns a controlled `DENIED` tool result the model must respect.
- **Grouping metadata.** Tag each `tool_started/completed` with the current plan step id so the panel
  can nest tool calls under their task, exactly as Antigravity groups tool calls within tasks.
- **Walkthrough assembly.** At terminal state, compose the walkthrough from the persisted plan, tool
  evidence, and citations — deterministic, from stored artifacts, not a second model call.
- Everything else already exists: the `ToolCallingManager` caps and bounded loop, per-tool governance,
  `agent_state` lifecycle (`QUEUED → PLANNING → RUNNING → VERIFYING → COMPLETED / FAILED / CANCELLED`),
  correlated audit, and terminal persistence that survives disconnect.

## 6. Frontend contract

Within the existing `modules/conversation/chat` structure:

- Plan details and tool activity live outside `MessageItem`, in the existing conversation workspace.
  The bubble renders the answer plus a compact state/action chip that points to **Tasks**.
- **Plan** renders the latest revision, progress, step title, description, and status.
- **Tasks** renders the Agent lifecycle, plan publication, runtime-confirmed tool events, and each
  image-generation execution separately.
  It never renders model-authored claims as actions.
- **Add** a Zod schema and store slice for `permission_required` and an approval action (POST the
  decision to the run), plus optimistic pending/approved/denied state.
- Grouping tool events under their plan-step id remains deferred until the backend emits that
  correlation metadata.
- **Restore** the full surface from persisted state on navigation/reload (follow live
  `plan_updated`/`agent_state`, fall back to the newest persisted assistant plan + evidence); never
  substitute preview-only steps.
- Keep the fixed-height layout with scrolling inside the panel, not the page.

## 7. What stays deferred

Antigravity's browser-driven verification (spin up a server, click flows, capture screenshots and
video walkthroughs) depends on capability families **C7–C9** (Workspace and system/computer control),
which are gated behind the Permission Engine's L3–L5 and their approval barrier. The surface is
designed to host those artifacts (screenshots, recordings) when those levels ship; until then the
walkthrough is text + citations + saved-knowledge + `mcp_*` evidence. Multi-agent orchestrator/worker
runs (Baeldung's orchestrator-workers / subagent patterns) are a later L4+ concern and would appear as
multiple task groups under one run in the same panel.

## 8. References

- Google Jules — [Review a plan](https://jules.google/docs/review-plan/),
  [activity types](https://jules.google/docs/api/reference/activities/),
  [running tasks](https://jules.google/docs/running-tasks/).
- Google Developers Blog — [Build with Google Antigravity](https://developers.googleblog.com/en/build-with-google-antigravity-our-new-agentic-development-platform/).
- Spring AI 2.0 — [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html),
  [Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html).
- Baeldung — [Building Effective Agents](https://www.baeldung.com/spring-ai-building-effective-agents),
  [Subagent Orchestration](https://www.baeldung.com/spring-ai-subagent-orchestration).
