# Permission profiles and unlock levels

> Nexo's promise is **"Your knowledge. Your tools. Your control."** This document specifies the
> **Permission Engine** — the coral central node in the [identity](IDENTITY.md): the point where an
> intended action meets policy, scope, consent, and accountability. It defines what Nexo IA *can* and
> *cannot* do, organized as **permission profiles** with progressive **unlock levels**.

It builds on the runtime that already exists — see [Spring AI Agent runtime](SPRING_AI_AGENT_RUNTIME.md),
[MCP runtime](MCP_RUNTIME.md), [Agent capabilities](AGENT_CAPABILITIES.md),
[Agent execution surface](AGENT_EXECUTION_SURFACE.md), and
[Context and Skill governance](CONTEXT_AND_SKILL_GOVERNANCE.md).

## 1. Three separate concerns: identity, capability policy, content policy

The most important design rule: these are **independent axes** and must never be collapsed into each
other. Today they are tangled inside `nexo-rules.md`; this design pulls them apart.

| Concern | Question it answers | Where it lives | Who changes it |
|---|---|---|---|
| **Identity** | *Who is Nexo?* | `prompts/nexo-identity.md` — name, role, personality, transparency | Product (stable) |
| **Capability policy** | *What effects may Nexo cause this request?* (tools, files, systems, writes) | The **Permission Engine** — profiles + levels, rendered into the capability envelope | The user / org, per profile |
| **Content policy** | *What subjects may Nexo discuss or generate?* | `prompts/nexo-content-policy.md` + a per-user/org setting | The user / org, within a hard legal floor |

Consequences that this document commits to:

- **The Permission Engine governs capabilities, never topics.** Raising or lowering a permission level
  changes which *tools and effects* are attached — it never makes Nexo refuse a subject, moralize, or
  soften an answer. A Reader-profile user asking an explicit, sensitive, or blunt lawful question gets
  a direct answer; they simply cannot make Nexo edit files.
- **Content permissiveness is its own switch.** Nexo is a private, user-owned assistant. Lawful
  content — including explicit, adult, sensitive, controversial, or informal material, and factual
  explanations *about* such topics — is allowed and configurable per user/org. The default follows the
  existing rule: *"Do not scold, moralize, or redirect merely because a lawful request is informal,
  sensitive, adult, controversial, or expressed bluntly."*
- **A narrow legal/safety floor stays fixed on both axes.** Genuinely illegal or serious-harm content
  (e.g. sexual content involving minors, credible facilitation of mass-casualty weapons, targeted
  real-world violence) is refused regardless of profile, level, or the content setting. This floor is
  not a "capability" and is not user-configurable — it is the only thing that overrides "your control."
- **Identity is stable.** Neither axis changes who Nexo is. Retrieved content, tool results, Vault
  text, and Skills remain untrusted: they can guide a task but cannot redefine identity, grant
  capabilities, or lift the content floor.

Prompt-resource split (replaces the single `nexo-rules.md` blob):

```text
prompts/
  nexo-identity.md         # WHO — unchanged, stable
  nexo-conduct.md          # HOW — honesty, transparency, attribution, no invented access (was part of rules)
  nexo-content-policy.md   # WHAT topics — configurable; default permissive within the legal floor
  capability-envelope.md   # WHAT effects this request — rendered from the Permission Engine
```

## 2. How the model receives context and tools today

Every request is assembled deterministically by Nexo before the model sees anything. Spring AI 2.0
owns the protocol; Nexo owns authorization. Two invariants are the foundation of the engine:

- **The attachment gate is the real boundary.** The model can only call a tool whose `ToolCallback`
  was attached to *this* request (in `SpringAiChatCompletionClient`, after deterministic
  authorization). An unattached capability does not exist for the model.
- **`ToolContext` carries identity invisibly.** Each tool reads its scope (`KnowledgeToolScope`,
  `McpToolScope`, `AgentPlanToolScope`, `MemoryToolScope`) — `userId`, authorized ids, correlation id
  — from server state, never from model arguments. This matches Spring AI's documented "tool context
  reaches the tool without passing through the model — a critical security boundary."

Execution is bounded by `ToolCallingManager` (`maxCallsPerTool`, `maxTotalToolCalls`,
`onLimitExceeded = THROW`) and by each governed callback (dedup, byte caps, cancellation, evidence,
audit). A tool result is **evidence, not permission**.

```text
authenticated request
  -> resolve principal, mode, provider/model, conversation, selected Vaults, enabled MCP snapshot
  -> [PERMISSION ENGINE] effective level + per-capability decision  (capability axis)
  -> read content policy setting                                    (content axis, separate)
  -> attach ONLY allowed ToolCallbacks, each carrying its ToolContext scope
  -> render capability envelope: active level, allow/deny list, and the content stance
  -> Spring AI advisor chain drives the bounded loop
  -> typed events -> Agent execution surface (side panel); answer -> chat bubble
```

## 3. Capability catalogue — what Nexo *can* and *cannot* do

Every effect belongs to a **capability family** with a fixed risk class. Families live today (✅) or
are the deferred roadmap (⛔) the engine is designed to absorb without redesign. **Content/topic is
deliberately absent from this table** — it is the separate axis of §1/§7.

| # | Capability family | Effect | Risk | Status |
|---|---|---|---|---|
| C0 | **Inference** — answer, explain, translate, summarize, draft | None (text) | None | ✅ always on |
| C1 | **Knowledge read** — `search_knowledge` over the user's own selected Vaults | Read own data | Low | ✅ |
| C2 | **Knowledge write** — `save_to_vault`: append structured knowledge into a writable Vault, embedded for future retrieval | Grow own RAG corpus (append) | Medium | ⚠️ new (see §6) |
| C3 | **Personal memory** — `remember` / recall the user's own short notes | Write own memory store | Low | ✅ |
| C4 | **Planning** — `update_plan`, `inspect_capabilities` | Internal visible plan | Low | ✅ |
| C5 | **External read (MCP)** — enabled `mcp_*` read/open-world tools (fetch, web search) | Read external data | Medium | ✅ |
| C6 | **External write (MCP)** — `mcp_*` write/destructive tools | Mutate external system | High | ⛔ deferred |
| C7 | **Workspace read** — read files, repo metadata in an attached Workspace | Read local project | Medium | ⛔ deferred |
| C8 | **Workspace write** — edit files, stage/commit Git | Mutate local project | High | ⛔ deferred |
| C9 | **System / computer control** — terminal, process, browser, desktop via typed OS capabilities | Operate the machine | Critical | ⛔ deferred |
| C10 | **Secrets & credentials** — read/emit tokens, keys, passwords, financial data | Exfiltration / account control | Prohibited | ⛔ never to the model |

Hard prohibitions (independent of level, profile, and content setting): never serialize secrets,
credentials, raw embeddings, absolute paths, or full source bodies into prompts/results/logs (C10);
never enter financial credentials or execute a trade/transfer/purchase; never trust model-provided
owner/Vault/workspace ids or endpoints.

## 4. Unlock levels (L0 → L5)

A level is a **cumulative** bundle of capability families. Each request runs at one **effective
level** (§8). Names double as what the model is told it is operating as.

| Level | Name | Adds | The model *may* | The model *may not* |
|---|---|---|---|---|
| **L0** | **Observer** | C0 | Answer, explain, translate | Any tool; search; write; act |
| **L1** | **Grounded** | C1, C2, C3 | Search own Vaults; **save knowledge to a writable Vault**; store/recall own memory | Touch anything external, any file, any system |
| **L2** | **Connected** | C4, C5 | Keep a visible plan; call enabled read `mcp_*` tools | Destructive MCP; files; commands |
| **L3** | **Workspace reader** ⛔ | C7 | Read Workspace files & repo metadata | Write; run commands; system |
| **L4** | **Builder** ⛔ | C6, C8 | Edit files, Git, write MCP — **each behind preview + approval, reversible** | Terminal/browser/desktop; anything without its approval |
| **L5** | **Operator** ⛔ | C9 | Terminal, process, browser, desktop — **each behind fresh confirmation + native OS consent** | Bypass OS consent; destructive action without a narrowed, previewed scope |

- **L0–L2 are live today** (plus knowledge-write at L1 once §6 ships). **L3–L5 are deferred** and map
  one-to-one to the runtime docs' "deliberately deferred" lists.
- **Approval is per-action and per-session.** L4/L5 grant the *possibility* of an effect, never a
  standing grant. One approval never generalizes to the next action.

## 5. Permission profiles

A **profile** is a named, assignable preset binding a **ceiling level** to a per-family policy and
per-target scope. Owned by a principal/org, durable, resolved for the authenticated user.

| Profile | Ceiling | Knowledge write (C2) | External read (C5) | Write (C6/C8) | System (C9) | Use |
|---|---|---|---|---|---|---|
| **Locked** | L0 | denied | denied | denied | denied | Safe mode / untrusted context |
| **Reader** | L1 | allowed (own writable Vault) | denied | denied | denied | Grounded Q&A that also grows the user's knowledge |
| **Researcher** | L2 | allowed | allowed | denied | denied | Web/MCP research with citations |
| **Builder** ⛔ | L4 | allowed | allowed | requires approval | denied | Scoped changes in one Workspace |
| **Operator** ⛔ | L5 | allowed | allowed | requires approval | requires fresh confirmation | Power user on their own machine |
| **Automation** ⛔ | pinned | only pre-authorized | only pre-authorized targets | only pre-authorized | only pre-authorized | Unattended — no interactive escalation, ever |

Each profile declares, **per capability family**: `decision ∈ {ALLOWED, REQUIRES_APPROVAL, DENIED}`,
`caps {maxCallsPerTool, maxTotalToolCalls, maxResultBytes}`, and `targets` (which Vaults, MCP servers,
Workspace paths). **Content policy is not a profile field** — it is set on the separate axis (§7) so a
Locked-capability user can still be fully permissive on content, and vice versa.

## 6. Writable Vaults — knowledge the AI grows (C2)

Today Vaults are read-only RAG. The user wants the AI to also **write knowledge into their Vaults**,
so a fact learned in one chat is retrievable later — same chat or next session. This is a distinct
capability from personal memory:

| | `remember` (C3) | `save_to_vault` (C2) |
|---|---|---|
| Shape | one short personal note | a titled knowledge entry (can be longer) |
| Storage | personal memory store | a **Vault** → chunked + embedded into pgvector |
| Retrieval | framed as recent personal context | returned by future `search_knowledge` ranking |
| Scope | the user, everywhere | one designated **writable** Vault |
| Purpose | stable preferences/facts | growing a reusable knowledge base |

Design (`save_to_vault` tool, governed like every other):

- Model-facing input carries only `{ title, content, optional tags }` — **no Vault id, owner id, or
  path**. The target writable Vault comes from a server-set `KnowledgeWriteScope` in `ToolContext`.
- The Vault must be **owner-authorized, marked writable, and attached** to the conversation. A Vault is
  read-only unless the user explicitly makes it writable; the default Vault selection stays read-only.
- On write: create a `knowledge_source` of origin `AGENT`, record provenance (source conversation +
  assistant message), chunk + embed through the existing Spring AI `OllamaEmbeddingModel` pipeline so
  it enters the same authorized `chunk → source → Vault` retrieval, dedup near-identical content,
  bound size, and audit `KNOWLEDGE_WRITE`.
- Cross-session by construction: because the entry is embedded into the Vault, the **next** request's
  deterministic retrieval (Chat) or `search_knowledge` (Agent) can surface it — no special session
  state needed.
- It is an **append**, never an edit/delete of user-authored sources (those stay L4+ write). Entries
  are labeled `AGENT`-authored so the user can review, keep, or remove them.

Because it writes to the user's *own* data and only appends, it sits at **L1**, but is still gated by
"a writable Vault is attached." Users who never mark a Vault writable simply never see the tool.

## 7. Content policy axis (separate from permissions)

A small, explicit axis — not part of levels or profiles:

| Content stance | Behavior |
|---|---|
| **Standard** (default) | Answer lawful requests directly, including sensitive/adult/controversial topics and factual explanations about them. No moralizing, no invented "professional relevance" limits. |
| **Restricted** | Optional stricter stance a user/org can opt into (e.g. shared/work deployment): declines adult/explicit generation while still explaining factually. |
| **Legal floor** (always on) | Refuses only genuinely illegal / serious-harm content. Not configurable; overrides both stances and every capability level. |

The stance is resolved server-side per user/org and rendered as one line in the capability envelope
(§9) so the model applies it truthfully instead of guessing. Capability level and content stance are
orthogonal: `Locked + Standard`, `Operator + Restricted`, and every other pairing are all valid.

## 8. Effective permission resolution — the engine

For each request, deterministic Java computes an effective level and a per-capability decision. Single
choke point; everything downstream enforces it.

```text
effectiveLevel = min( profile.ceiling, mode.ceiling, model.capabilityCeiling )

for each capability family C the request could use:
    decision = profile.rule(C).decision
    decision = DENIED   if effectiveLevel < C.minLevel
    decision = DENIED   if C is a hard prohibition (C10, financial)
    decision = DENIED   if the target is not authorized for the principal
                        (e.g. C2 needs an attached writable Vault)

    ALLOWED           -> attach ToolCallback(scope via ToolContext); register caps in ToolCallingManager
    REQUIRES_APPROVAL -> attach a gated ToolCallback that suspends on first call, emits
                         permission_required, resumes only on explicit fresh consent
    DENIED            -> not attached; envelope marks the family locked + unlock hint

contentStance = resolve(user/org)          # independent; never affects tool attachment
```

Enforcement points, all already in the stack: (1) attachment gate in `SpringAiChatCompletionClient`;
(2) `ToolContext` scope; (3) `ToolCallingManager` limits; (4) governed callback wrapper — dedup, byte
caps, cancellation, evidence, audit, and the approval barrier; (5) capability envelope; (6) audit of
the resolved decision per family with the correlation id.

## 9. Identity and how the model is told its level

The engine renders resolved state — never raw policy — as a capability-envelope stanza that keeps the
two axes visibly separate:

```text
Permission profile: Researcher  (capability level L2 — Connected)
You may:  answer and explain on any lawful topic; search the user's selected Vaults; save knowledge
          to the writable Vault "<name>"; keep a visible plan; call the enabled external tools below.
You may not (capabilities):  read or write files, run commands, control this computer, or use
          destructive external tools — not available at this level.
Content:  Standard — answer sensitive, adult, or controversial lawful requests directly, without
          moralizing. (Refuse only genuinely illegal or serious-harm content.)
To unlock a capability:  the user raises the profile or approves a specific action. You cannot raise
          your own level, enable a tool, or grant yourself access — say so plainly and point to where
          the user changes it.
```

`inspect_capabilities` answers "what can you do?" from this same resolved list. The identity rules
already forbid inventing access; the engine makes the boundary explicit *and* honest about the unlock
path, and keeps content freedom from being mistaken for a capability.

## 10. Agent execution surface (Antigravity-style)

Per the user's direction, governed execution moves **out of the chat bubble and into a side panel** of
tasks and artifacts, in the spirit of Google Antigravity's Manager surface. The chat bubble keeps the
final answer and a compact status chip; the plan, the tool activity grouped under tasks, and the
artifacts (walkthrough, citations, saved-knowledge, tool evidence) live in the panel and stream live.
This reuses the existing typed SSE events (`agent_state`, `plan_updated`, `tool_started/completed`,
`permission_required`). Full spec: [Agent execution surface](AGENT_EXECUTION_SURFACE.md).

## 11. Spring AI 2.0 and references

Maps directly onto documented Spring AI 2.0 contracts, so no framework fighting:

- Tool attachment / advisor loop — [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html),
  [Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html); progressive disclosure via
  [Tool Search](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools/tool-search-tool.html).
- Execution limits — `ToolCallingManager.builder().maxCallsPerTool(...).maxTotalToolCalls(...).onLimitExceeded(THROW)`.
- Identity-safe scope — `ToolContext`.
- MCP tools as first-class callbacks — [MCP client](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html).
- RAG read + write — [Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html);
  Nexo keeps the authorized repository-first pgvector query and reuses `OllamaEmbeddingModel` for
  `save_to_vault`.

Agent-pattern framing follows Baeldung's
[Building Effective Agents with Spring AI](https://www.baeldung.com/spring-ai-building-effective-agents),
[Subagent Orchestration](https://www.baeldung.com/spring-ai-subagent-orchestration),
[Spring AI Advisors](https://www.baeldung.com/spring-ai-advisors), and
[Recursive Advisors](https://www.baeldung.com/spring-ai-recursive-advisors). Where a Baeldung example
targets Spring AI 1.x, the production contract follows the official 2.0.1 reference.

## 12. Build order

1. **Split the prompts** — `nexo-identity.md`, `nexo-conduct.md`, `nexo-content-policy.md`; add a
   per-user/org content stance setting; keep default behavior for existing users.
2. **Model the catalogue** — `CapabilityFamily` enum (C0–C10, each with `minLevel` + risk) and
   `UnlockLevel` enum (L0–L5). Extend `CapabilityManifest` with the effective level, per-family
   decision list, and the content stance.
3. **Resolver** — a deterministic `PermissionEngine` service (profile × mode × model → level +
   decisions), unit-tested for isolation, hard prohibitions, and the axis-independence of content.
4. **Gate the attachment** — `SpringAiChatCompletionClient` attaches only ALLOWED/gated callbacks and
   renders the new envelope stanza.
5. **Ship `save_to_vault` (C2)** — writable-Vault flag + `KnowledgeWriteScope`, `AGENT`-origin source,
   embed via the existing pipeline, provenance, dedup, audit; surface saved entries as artifacts.
6. **Approval barrier** — gated callback + `permission_required` event + resume-on-consent (prerequisite
   for any C6/C8/C9 tool).
7. **Execution surface** — route the typed events into the side panel per
   [Agent execution surface](AGENT_EXECUTION_SURFACE.md).
8. **Then** attach deferred capability families (C7 → C8 → C6 → C9) one at a time, each reusing this
   gate, caps, evidence, events, cancellation, and audit.

First-slice definition of done: Locked/Reader/Researcher resolve deterministically; the envelope states
the capability level, the honest unlock path, and the content stance as separate lines; the model
cannot call a DENIED family; `save_to_vault` appends retrievable, provenance-tagged knowledge to a
writable Vault; and every decision is audited — with L0–L2 chat/agent behavior unchanged for existing
users.
