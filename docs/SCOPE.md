# Product scope

## In scope

- Conversation interface.
- Organizations, users, teams, roles, sessions, and resource access control.
- Provider registry, Privacy Gateway, token usage, budgets, and quotas.
- Local and centralized deployment with paired cross-platform Nexo Companion devices.
- Device inventory, access policy, execution snapshots, and auditable activity.
- Integration with local models.
- Response streaming.
- History and context management.
- Interlinked Knowledge Vaults and RAG over authorized local sources.
- Controlled local tools.
- Governed inspection and modification of authorized Project databases.
- Frontend Vault Explorer for personal and shared knowledge.
- Controlled computer interaction on Linux, Windows, and macOS through platform adapters.
- Permission Engine.
- MCP client and catalog.
- Agent loop, memory, and auditing.
- Principal-scoped context, cross-chat personal memory, governed shared memory, and user-created Skills.
- Scheduled tasks with isolated runs and pre-authorized permission scopes.
- A unified calendar for automations, Cowork milestones, checkpoints, and approval deadlines.
- Image generation through controlled local or explicitly enabled remote providers.
- Tests and evaluations for responses and actions.

## Outside the first development cycle

- Video generation and advanced image editing.
- Speech recognition and synthesis.
- Unrestricted or permission-bypassing operating-system automation.
- Mobile applications.
- Distributed architecture and large-scale infrastructure.
- Unauthenticated public deployment.

These capabilities may be studied later. Keeping them outside the first cycle protects the learning
goal and reduces the number of simultaneous problems.

## First usable product

Release `0.1` is a local or private-network application in which an Owner bootstraps the installation,
creates a second user, configures organization-owned Ollama, and proves isolated persistent chat,
streaming, cancellation, token attribution, administration, and audit. The authoritative scope and
acceptance gates are in [MVP and release strategy](MVP_AND_RELEASE_STRATEGY.md).
