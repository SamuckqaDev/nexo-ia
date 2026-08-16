# Enterprise architecture

## Product shape

Nexo IA is designed as a team product even when deployed for one person. The initial implementation
remains a modular monolith, but identity, ownership, isolation, policy, usage, and audit are explicit
domain concepts from the beginning.

```text
Nexo instance
  -> organization
     -> owner, administrators, teams, members, and service accounts
     -> projects, Workspaces, Knowledge Vaults, Skills, and providers
     -> policies, budgets, autonomous runs, devices, and audit
```

The installation bootstrap creates a **Nexo Owner**, not an operating-system root account. The Owner
creates or invites other users and delegates narrowly scoped administration.

## Identity and access

Authentication uses Spring Security and revocable server-side sessions. Passwords are stored only as
salted, adaptive hashes; raw passwords, recovery secrets, and provider credentials never enter logs.
MFA and external OIDC providers may be added without changing resource ownership.

Authorization combines roles, resource ownership, explicit access-control entries, capability
policies, and data-transmission policies. Personal conversations and memories remain isolated unless
shared explicitly. Organization, team, project, Vault, Workspace, provider, device, and run access
are evaluated independently.

Context assembly is principal-scoped and versioned. Sharing a Project or Skill never implicitly
shares related Vaults, Workspaces, memories, secrets, provider credentials, devices, or conversations.
Scheduled work runs as a recorded user or service account rather than inheriting ambient context.

## Providers, privacy, and usage

The Provider Registry supports local, organization-hosted, and explicitly enabled remote providers.
Every request records model, provider, processing location, user, team, project, run, input/output
token usage when available, latency, and estimated cost. Budgets and quotas may apply per request,
run, user, team, project, model, or organization.

A Privacy Gateway applies data classification, context minimization, secret and personal-data
detection, redaction, provider policy, and optional transmission preview before remote processing.
Remote inference necessarily transmits the selected prompt and context; Nexo IA must never describe
it as private merely because redaction was attempted. Remote fallback is never silent.

## Governed autonomy

Autonomy is a policy, not unrestricted access. Manual, assisted, supervised-autonomous,
pre-authorized, and unattended-scheduled runs share the same plan, Permission Engine, Privacy
Gateway, budgets, timeouts, evidence, audit, pause, cancellation, and emergency-stop controls.

## Tool strategy

Nexo IA prefers portable Java or operating-system capabilities, then maintained free and open-source
tools, then free tools with stable APIs or CLIs. A project-owned MCP server may expose a tool through
typed, validated, governed contracts when appropriate. MCP does not bypass a license, authentication,
usage limit, or price. Paid services remain optional and require explicit configuration.

Core filesystem, process, Workspace, Vault, permission, privacy, and audit capabilities remain native.
Independent product integrations are candidates for the MCP Hub and reusable Nexo MCP toolkit.

## Deployment

Nexo IA supports local and client-server deployment. A central server or NVIDIA DGX may host the Java
application, models, RAG indexes, orchestration, policy, and audit. A Nexo Companion installed on an
authorized Linux, Windows, or macOS device performs local effects that a browser cannot.

```text
Web interface -> Nexo Server or DGX -> secure task channel -> Nexo Companion -> user device
```

Every operation records its processing location separately from its execution location. Centralized
deployment does not silently grant the server control of a user's device.

Local releases provide Fedora Silverblue, conventional Linux, Windows, and macOS profiles from the
same containerized Nexo Server build. Ollama remains host-native by default, while the later Nexo
Companion is a signed native component for endpoint effects. See
[Cross-platform build and distribution profiles](DISTRIBUTION_BUILDS.md).
