# Context isolation and Skill governance

## Purpose

Nexo IA assembles model context for one authenticated principal, organization, objective, and run at
a time. Context is a temporary authorized view; it is not permission, ownership, or an automatic copy
of every resource the user can access.

## Context envelope

Every model request receives an immutable, auditable context envelope containing identifiers and
versions for:

```text
System security policy
  -> organization policy
  -> authenticated user and active role
  -> Project and Cowork objective
  -> authorized Knowledge Vault retrieval
  -> authorized Workspace observations
  -> scoped personal or shared Memory
  -> active instruction sources
  -> selected Skills
  -> Conversation and current task
  -> model, provider, privacy policy, and budgets
```

Security policy is always authoritative. More specific task instructions may refine behavior but
cannot weaken organization policy, Privacy Gateway rules, Permission Engine decisions, resource
grants, or execution limits.

## User and resource isolation

- Personal conversations, memories, Skills, provider credentials, and private Vaults are visible only
  to their owner unless shared explicitly.
- Organization and team resources require an active grant for both discovery and use.
- A shared Project does not implicitly share every related Vault, Workspace, Memory, Skill, secret,
  device, conversation, or provider.
- Retrieval applies user, organization, Project, Vault, source, and sensitivity filters before
  candidate content reaches ranking or the model.
- Cached prompts, retrieval results, embeddings with protected metadata, and model responses use the
  same isolation key and retention policy as their originating context.
- Background and scheduled runs execute as a recorded principal or service account with explicit
  grants; they never inherit the last interactive user's context.

## Memory scopes

Memory scopes are `session`, `personal`, `project`, `team`, and `organization`. Personal memory may be
selected across the owner's chats when relevant and allowed. Shared memory requires explicit creation
or promotion, provenance, visibility, and an authorized scope. Secrets and credentials are never
memory.

The user can inspect, approve, edit, rescope, expire, disable, export, and delete memories, and can
see which memories influenced a run. Deletion removes derived retrieval entries and caches according
to the retention policy.

## Skill ownership and scopes

Skills have an owner, version, origin, trust state, visibility, compatibility, dependencies, and
declared resource and capability requirements. Supported scopes are:

- `built_in`: shipped and maintained with Nexo IA;
- `organization`: published under organization governance;
- `team`: shared with selected teams;
- `project`: available only in an explicit Project;
- `workspace`: discovered from an authorized Workspace;
- `personal`: private to its creator;
- `session`: temporary and discarded or archived with the session.

Creating, viewing, editing, testing, publishing, installing, enabling, executing, and deleting a
Skill are separate permissions. A personal Skill does not become visible to administrators as content
merely because they administer the installation; access follows the organization's documented audit
and support policy.

## Skill activation and access

Skill discovery first filters by identity, scope, enabled state, trust, compatibility, Project,
Workspace, and policy. Explicit invocation identifies a desired Skill but does not bypass those
filters. Implicit activation records why a Skill matched and may be disabled per user, Project, or
organization.

A Skill provides instructions and deterministic resources; it grants no access. Sharing or
publishing a Skill never shares its author's Vaults, Workspaces, memories, secrets, providers,
devices, or previous outputs. Required resources are resolved again for the current principal and run
through access control, the Privacy Gateway, and the Permission Engine.

The current frontend preview supports explicit activation by typing `/` in Chat. Selecting a Skill
adds its bounded method and expected output to that message and records a visible Skill badge on the
rendered user turn. This makes the method usable with the current provider without pretending that
publication, trust evaluation, dependency authorization, automatic discovery, or the governed Skill
runtime is already implemented.

## Context minimization and audit

The Context Assembler selects only relevant authorized content within explicit token and privacy
budgets. It records source identifiers, versions, selection reasons, truncation, redaction, selected
Skills, retrieved Vault passages, memory references, provider, token usage, and policy decisions.
The inspector exposes this operational provenance without revealing secrets, other users' resource
existence, or private model reasoning.

Cross-user isolation, cache-key isolation, permission revocation, Skill sharing, scheduled-principal,
and negative retrieval tests are release-blocking security tests.
