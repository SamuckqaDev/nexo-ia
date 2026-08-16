# Device management and execution audit

## Nexo Companion

The Nexo Companion is a cross-platform endpoint agent paired with a Nexo Server. It discovers local
capabilities and executes authorized filesystem, process, application, notification, browser, and
desktop operations on the target device. The Companion initiates the secure channel so ordinary
deployments do not expose an inbound control port on the endpoint.

Pairing binds a user-confirmed device identity to an organization and owner. Device credentials are
unique, short-lived where applicable, rotatable, and immediately revocable. Sensitive actions may
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
