# Device management and execution audit

## Implemented local-runtime foundation

Nexo now includes the first native Companion implementation under `desktop/`. It is an Electron
endpoint runtime, not a browser filesystem bridge. An authenticated web session creates a
single-use, ten-minute pairing code; the Desktop exchanges it for a revocable opaque credential and
opens an outbound authenticated WebSocket using protocol `nexo.runtime.v1`. The server stores only
the credential hash. The raw credential and absolute workspace paths remain encrypted by the
operating-system credential service in the Desktop application data directory.

The implemented capability set is deliberately read-only:

- bounded directory listing and literal text search;
- bounded UTF-8 file excerpts;
- stack and repository inspection;
- Git status and one-file unstaged diff.

The server sends only an opaque local binding identifier and workspace-relative tool input. The
Desktop resolves the binding to the real local path, rejects traversal, symlink escape, secrets,
binary/oversize files, ignored dependency/build trees, shell interpolation, and model-authored Git
arguments. It reports capabilities and heartbeats, refreshes folder fingerprints every minute, and
reconnects without requiring an open browser tab. Device revocation closes the active channel and
prevents the stored credential from authenticating again.

Compose remains loopback-only by default. An operator may set `NEXO_BIND_ADDRESS=0.0.0.0` to make
the web gateway and development endpoints reachable from a trusted private network. The production
Nginx gateway forwards the HTTP upgrade for `/api/v1/device-runtime/connect`, so the outbound
Companion WebSocket reaches the same authenticated server origin used by the UI. A second machine
can therefore load the server renderer (for the current unsigned build, set
`NEXO_RENDERER_URL=http://<server-ip>:5173`) and pair without copying its repository to the server.
Public or untrusted-network deployment requires HTTPS/WSS, secure cookies, a trusted TLS proxy and
firewall policy; setting a non-loopback bind by itself is not a production security configuration.

This foundation does not yet execute arbitrary shell commands, write files, mutate Git, delegate to
worker models, elevate operating-system privileges, or present local approval dialogs. Those effects
require the policy, approval, sandbox, rollback, and artifact layers described below before they may
be exposed as Spring AI tools.

## Nexo Companion

The Nexo Companion is a cross-platform endpoint agent paired with a Nexo Server. It discovers local
capabilities and executes authorized filesystem, process, application, notification, browser, and
desktop operations on the target device. The Companion initiates the secure channel so ordinary
deployments do not expose an inbound control port on the endpoint.

Pairing binds a user-confirmed device identity to an organization and owner. Device credentials are
unique, short-lived where applicable, rotatable, and immediately revocable. The implemented pairing
code is short-lived and single-use; the device credential is revocable but rotation is still
pending. Sensitive actions may
require confirmation on the target device even when organization policy permits them.

## Device inventory

With an explicit organization policy and user notice, Nexo IA records:

- device identifier, display name, hostname, owner, organization, teams, and device type;
- manufacturer and model when reliably available;
- operating system, distribution, version, architecture, and timezone;
- CPU model, cores and threads, total memory, GPUs and video memory;
- storage volumes, capacity and available space;
- server-observed IP, permitted local network information, and last connection;
- Companion version, health, heartbeat, supported capabilities, and authorization state.

Inventory has three representations: the latest known state, an auditable history of material
changes, and the immutable relevant snapshot attached to an execution. Volatile values such as IP,
free memory, and disk space are timestamped observations, not permanent identity.

## Device access policy

Policy declares who may view, request execution, approve effects, administer, or revoke a device;
which Workspaces and capabilities are available; allowed hours, providers, budgets, and autonomy
levels; and whether local confirmation is required. Administrator authorization never implies
operating-system elevation, and elevation is never automatic.

## Execution record

Every effect links:

- requester, organization, team, Project, Cowork session, plan, task, Skill, or automation;
- coordinating server and execution device;
- AI model, model version when available, provider, and processing location;
- device identity, observed IP, and relevant inventory snapshot;
- normalized capability, tool or MCP server, arguments, targets, and Workspace;
- policy evaluation, permission decision, approver, local confirmation, and expiration;
- start, finish, status, result, evidence, affected artifacts, and failure reason;
- token usage, estimated cost, duration, attempts, and relevant resource consumption.

The audit trail records metadata necessary for accountability while redacting credentials, secret
values, private prompts, and unrelated environment data. Access to inventory and audit exports is
itself audited.

## Privacy, retention, and integrity

Nexo IA avoids invasive hardware identifiers unless a documented security need and policy justify
them. Views mask IP addresses, paths, and inventory fields for unauthorized roles. Organization
retention rules define snapshot and event lifetime. Companion events use authenticated transport,
anti-replay data, correlation identifiers, and integrity protection. Revocation and emergency stop
prevent new work and cancel cancellable active work on the target device.
