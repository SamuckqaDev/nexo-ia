# Organizations, roles and governance

> How Nexo becomes a **multi-user, team product** without weakening isolation. A **Group** is a
> complete organization: it owns members, shared knowledge, a token budget, media, and artifacts, and
> it carries its own governance — roles, capability profiles, and a content matrix. This layers on the
> capability model in [Permission profiles and unlock levels](PERMISSION_PROFILES.md); that document
> owns *what the AI may do*, this one owns *who governs whom and which resources belong to the team*.

## 1. The Group is the organization

A **Group** (team / organization) is the tenant boundary. Everything a team shares — knowledge,
tokens, media, artifacts — belongs to the Group, and everything a person does can happen either in
their **personal space** or inside an **active Group context**. A user may belong to several Groups
and still keep a personal space; the active context decides which resources and rules apply to a
request.

```text
User ── belongs to ──> Group A ── owns ──> knowledge · token budget · media · artifacts · governance
     └─ personal space (private)          Group B ── owns ──> (isolated from Group A)
```

## 2. Resource ownership: USER or GROUP

Every ownable resource carries an owner discriminator, so the same isolation query serves both cases:

```text
owner_type: USER | GROUP
owner_id:   the user id or the group id
```

Applies to Vaults, media, artifacts, conversations, and usage records. **Personal** resources are
private to the user; **Group** resources are shared with that Group's members and invisible to any
other Group. Authorization always resolves for the authenticated principal *and* the active Group —
a member of Group A can never read Group B's resources, and a personal resource is never exposed to a
Group.

## 3. What a Group owns

| Resource | Meaning | Isolation |
|---|---|---|
| **Members** | users with a role + assigned capability profile + content matrix in this Group | per Group |
| **Knowledge** | Group Vaults (shared corpus); read via RAG, appended via `save_to_vault` per profile | per Group |
| **Token budget** | a quota all members' model usage draws from, tracked per period | per Group |
| **Media** | generated images and files, a shared Group library | per Group |
| **Artifacts** | agent deliverables — saved knowledge, reports, diffs, walkthroughs | per Group |
| **Settings** | default profile for new members, default content matrix, allowed providers/models | per Group |

## 4. Roles and delegation (the authority axis)

Authority (**who governs whom**) is a separate axis from capability (**what the AI may do**). Three
roles, and one invariant: *you may only grant what is at or below your own — in role and in ceiling.*

| | **ROOT** (system owner) | **ADMIN** | **MEMBER** |
|---|---|---|---|
| **Define** a profile / content matrix (author policy) | ✅ only root | ❌ | ❌ |
| Appoint an **admin** (grant the admin role) | ✅ only root | ❌ | ❌ |
| **Create groups** and add people to them | ✅ anywhere | ✅ (becomes that group's admin) | ❌ |
| Add/remove **members** | ✅ any group | ✅ groups they administer | ❌ |
| **Assign** an existing profile to a member | ✅ any | ✅ **≤ their own ceiling**, in their groups | ❌ |
| Set a Group's **token budget / settings** | ✅ any | ✅ their groups, within what root allotted | ❌ |
| Do anything, no boundaries, no approval prompts | ✅ | ❌ | ❌ |

The distinction the model turns on: **authoring policy is root-only; administering it is delegated.**
An admin never invents a new profile or content matrix — they *assign* the profiles root defined and
*manage* membership and Group resources, always bounded by their own ceiling.

Delegation invariants (enforced server-side, deterministically, never on the client):

1. **No escalation by delegation** — an actor may only assign `role(target) < role(actor)` and
   `ceiling(target) ≤ ceiling(actor)`.
2. **No self-elevation** — nobody raises their own role or ceiling.
3. **Group isolation** — an admin governs only the Groups they administer, never another admin's.
4. **Root is unbounded but audited** — root has no limits, yet every root action is recorded, because
   accountability is not the same as restriction.

## 5. Capability profiles

The capability a member gets is a **profile** (Locked / Reader / Researcher / Builder / Operator) that
resolves to an unlock level (L0–L5) — the full mechanism is in
[Permission profiles and unlock levels](PERMISSION_PROFILES.md). Root **defines** what each profile
means; an admin **assigns** an existing one to a member, never above their own ceiling. A member's
effective capability is the most restrictive of their personal profile, their Group's member ceiling,
and their role.

## 6. Content matrix (per-profile, per-area)

Content — *what subjects may be generated* — stays a separate axis from capability, but it is **not a
single toggle**. Each profile carries a **content matrix**: for each sensitive-but-lawful **area**, an
allowance.

| Allowance | Behavior |
|---|---|
| **FULL** | generate freely (lawful content) |
| **PARTIAL** | explain factually / clinically, but do not generate graphic material |
| **BLOCK** | refuse, stating the area is not enabled for this profile |

Starter areas (extensible): `sexual-explicit`, `graphic-violence`, `strong-language`,
`mature-themes`, `medical-explicit`. A profile sets each area independently:

```text
ROOT profile:    every area = FULL          (any lawful content, no questions)
MEMBER profile:  sexual-explicit = BLOCK · graphic-violence = PARTIAL · strong-language = FULL · medical-explicit = FULL
"Creator" profile: sexual-explicit = PARTIAL · ...   (the "partial, depends on the area" case)
```

The matrix is resolved per request and rendered into the content-policy system message (an evolution
of `nexo-content-policy.md`): the model is told exactly which areas it may generate, may only explain,
or must refuse. Default first cut is **prompt-level** (the model obeys the rendered matrix, like
today's content stance) — a classifier that detects the area up front can be added later if stricter
enforcement is needed. In a Group, the effective allowance for an area is the **more restrictive** of
the member's profile and the Group's matrix.

**The fixed legal floor is not an area.** Genuinely illegal or serious-harm content (sexual content
involving minors, credible mass-harm facilitation) is refused for **everyone, including root**. It is
architectural, not a tunable allowance — the one line that protects the operator and cannot be
delegated away.

## 7. Token budget and quota

A Group has a **token budget** for a period; all members' model usage draws from it.

- `group.token_budget` (limit + period) — set by an admin within what root allotted, or by root.
- Every usage record is attributed to `(group_id, user_id, provider, model)`, extending the existing
  usage module.
- The budget is **enforced deterministically** in the request pipeline: a member's request is admitted
  only while the Group has remaining budget; exhaustion produces a controlled "budget exhausted" state,
  never a silent failure. Root and personal usage are tracked separately and are not capped by a Group
  budget.
- Admins see their Group's consumption; root sees all. This reuses the existing usage accounting and
  the pre/post-request accounting points, adding the Group attribution and the gate.

## 8. Active-organization context

A request resolves its scope before anything else:

```text
authenticated request
  -> resolve principal + ACTIVE GROUP (or personal space)
  -> resources = that scope's Vaults, media, artifacts
  -> budget   = that Group's token budget (personal space: personal accounting)
  -> profile  = the member's effective profile in that Group
  -> content  = the member's effective content matrix in that Group
  -> [PERMISSION ENGINE] effective level + decisions + content allowances
  -> ... existing pipeline ...
```

Switching the active Group is like switching a workspace: knowledge, tokens, media, artifacts, the
effective profile, and the content matrix all re-resolve to that Group. Client-supplied ids are never
the authorization source — the active Group is resolved and authorized server-side.

## 9. Effective resolution (master formula)

```text
# authority / who can grant — checked on admin actions
grant allowed iff role(actor) > role(target) AND ceiling(actor) >= ceiling(target)

# capability — per request (extends PERMISSION_PROFILES §8)
roleCeiling      = ROOT ? UNBOUNDED : ceilingForRole(role)
effectiveCeiling = min(roleCeiling, personalProfile.ceiling, activeGroup.memberCeiling)
effectiveLevel   = min(effectiveCeiling, modeCeiling, modelCapability)
escalation       = ROOT ? auto-approve : REQUIRES_APPROVAL stays

# content — per request, per area
allowance(area)  = ROOT ? FULL : mostRestrictive(profile.matrix[area], activeGroup.matrix[area])
                   (legal floor always refused, independent of the above)

# budget — per request
admit iff activeGroup.remainingBudget > 0   (personal space: personal accounting; root: uncapped)
```

Root is the clean special case throughout: unbounded ceiling, auto-approved escalation, FULL content,
uncapped budget, no approval prompts — "does what I ask without questioning" — while still audited.

## 10. Security invariants

- Resolve every Group, member, Vault, media item, artifact, budget, and profile for the authenticated
  principal *and* the active Group; never trust client-provided group/owner ids.
- Group resources never leak across Groups; personal resources never leak into a Group.
- Delegation is bounded (§4) and every grant/appointment/budget change is audited with granter,
  grantee, and the value granted.
- The content legal floor and the capability hard-prohibitions (secrets, financial) hold for everyone,
  including root.
- A profile or content matrix change takes effect on the next request; it never retroactively rewrites
  stored history or evidence.

## 11. Data model (additive)

```text
group(id, name, created_by, token_budget_limit, token_budget_period, default_profile_id, created_at)
group_membership(id, group_id, user_id, group_role[ADMIN|MEMBER], assigned_profile_id, joined_at)
user(... , system_role[ROOT|ADMIN|MEMBER], personal_profile_id)
permission_profile(... , content_matrix_json)          # areas -> FULL|PARTIAL|BLOCK  (root-authored)
knowledge_vault(... , owner_type[USER|GROUP], owner_id) # group-owned Vaults
media_asset(... , owner_type, owner_id)                 # group media library
artifact(id, owner_type, owner_id, kind, ref, created_at)
conversation(... , owner_type, owner_id)                # personal or group-scoped
usage_event(... , group_id nullable)                    # group attribution + quota
audit_event(... , actor_id, subject_id, action, granted_value)  # delegation trail
```

## 12. Build order

1. **Authority & profiles** — `system_role`, root-authored `permission_profile` (capability +
   content matrix), assign a personal profile at user creation, and the deterministic
   `PermissionAdminService` that enforces the delegation invariants (§4). No groups yet.
2. **Groups & membership** — `group`, `group_membership`, group roles; root/admin create groups and
   manage members; assign existing profiles bounded by the actor's ceiling.
3. **Active-Group context** — resolve the active Group per request and thread it into the existing
   `PermissionEngine` resolution (§9).
4. **Group knowledge** — `owner_type` on Vaults; extend the authorized retrieval/write join to
   `owner OR member-of-owning-group`, preserving isolation.
5. **Token budget** — Group attribution on usage + the pre/post-request quota gate.
6. **Content matrix** — render the per-profile/per-area matrix into the content-policy system message.
7. **Media & artifacts** — group ownership + shared libraries surfaced in the workspace panel.
8. **Root fast-path** — auto-approved escalation, FULL content, uncapped budget, still audited.

First slice done means: root defines profiles; a user is created with a bounded profile; an admin
creates a group and adds a member with a profile ≤ the admin's; the member's requests resolve the
Group's knowledge and the effective capability + content matrix; every grant is audited — with
today's single-user behavior unchanged for anyone not in a Group.

The backend names this first Group entity `Team`. Migration V33 removes the legacy user-only Vault
foreign key, keeps `(owner_type, owner_id)` indexed, and adds the user references required by Teams
and memberships. Team, membership, and Team-owned Vault creation flush before their timestamped API
responses are assembled, so strict clients never receive database-generated dates as `null`.
