# Agent execution surface

> How Nexo shows agent work: not as noise inside the chat bubble, but as a reviewable **side panel of
> tasks and artifacts**, in the spirit of Google Antigravity's Manager surface. The chat stays a
> conversation; the *work* becomes inspectable deliverables. This realizes the identity promise —
> **"Your knowledge. Your tools. Your control"** — by making every governed step visible and auditable
> without drowning the reply.

Companion to [Permission profiles and unlock levels](PERMISSION_PROFILES.md),
[Spring AI Agent runtime](SPRING_AI_AGENT_RUNTIME.md), and [MCP runtime](MCP_RUNTIME.md).

## 1. The problem with in-bubble execution

Today the plan revisions and tool activity render inside the assistant message. As soon as an Agent
run makes several tool calls, the bubble becomes a scroll of low-level steps and the actual answer is
buried. Antigravity's insight applies directly: users should **review high-level deliverables at key
milestones**, not watch every individual tool call synchronously.

## 2. Two surfaces, one run

| Surface | Holds | Purpose |
|---|---|---|
| **Chat bubble** | the final answer + a compact **status chip** (running / needs approval / done / failed, with elapsed time and a "view activity" affordance) | stays a readable conversation |
| **Activity panel** (side menu, next to Plan/Memory) | the live plan, tasks with their grouped tool calls, and artifacts | the "mission control" for the run |

The panel is the existing conversation workspace, promoted from today's Plan/Memory sections into a
full **Activity** surface. It follows live events while connected and restores from persisted state
after navigation or reload — the browser SSE connection is an **observer, not the execution owner**
(leaving the chat never cancels the server run).

## 3. Artifacts (Antigravity-style deliverables)

Each Agent run produces reviewable artifacts, streamed into the panel as they materialize:

| Artifact | Nexo source | When |
|---|---|---|
| **Task list / Plan** | `update_plan` revisions (≤12 steps, one `IN_PROGRESS`) | published at run start, revised live |
| **Task activity** | tool calls **grouped under the task** that triggered them, each with a one-line summary, status, and elapsed time | during execution |
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
  plan_updated          -> panel: Plan/Task list
  tool_started          -> panel: open a step under the active task
  tool_completed        -> panel: close the step with summary + evidence artifact
  permission_required   -> panel: approval card (blocks that capability until answered)
  agent_state           -> chat status chip + panel header (QUEUED..COMPLETED/FAILED/CANCELLED)
  usage / completed     -> chat bubble: final answer + walkthrough artifact
```

New event to add for the permission engine: **`permission_required`** (family, target label, reason,
execution id). Its Approve/Deny reply resumes or denies exactly one gated tool call — per-action,
per-session, never generalized (see [Permission profiles](PERMISSION_PROFILES.md) §8).

## 5. Backend adjustments (Spring AI 2.0)

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

## 6. Frontend adjustments

Within the existing `modules/conversation/chat` structure:

- **Move** plan + tool activity out of `MessageItem` into an **Activity** panel component beside the
  current Plan/Memory workspace sections; the bubble renders answer + a status chip only.
- **Add** a Zod schema and store slice for `permission_required` and an approval action (POST the
  decision to the run), plus optimistic pending/approved/denied state.
- **Group** tool events by plan-step id in the store; render tasks with nested, collapsible steps and
  their evidence artifacts.
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

- Google Antigravity — [Manager surface & artifacts](https://antigravity.google/docs/artifacts/),
  [walkthrough](https://antigravity.google/docs/walkthrough/),
  [introduction](https://antigravity.google/blog/introducing-google-antigravity).
- Spring AI 2.0 — [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html),
  [Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html).
- Baeldung — [Building Effective Agents](https://www.baeldung.com/spring-ai-building-effective-agents),
  [Subagent Orchestration](https://www.baeldung.com/spring-ai-subagent-orchestration).
