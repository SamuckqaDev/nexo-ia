# Server workspaces

Nexo project execution starts from a server-owned Workspace. The browser selects a persisted
Workspace for one conversation; it never sends a local folder handle, an absolute filesystem path,
or a command for the model to execute. This makes project context stable across browser sessions and
devices while keeping authorization and audit on the Nexo server.

## Storage modes

| Mode | Resolution | First-release rule |
|---|---|---|
| `MANAGED` | `{managed-root}/{owner-id}/{workspace-id}` | Nexo creates and owns the directory. |
| `MOUNTED` | `{import-root}/{relativePath}` | Existing server project; Owner only; disabled when the import root is empty. |
| `UNBOUND` | no filesystem path | Metadata may exist, but no file or Agent tool is available. |

`relativePath` must remain inside the configured import root after normalization and real-path
resolution. The resolver rejects absolute input, traversal, symlink escape, missing paths, and paths
outside the authorized Workspace. API responses and tool results expose only Workspace-relative
paths.

Configuration:

```properties
NEXO_WORKSPACE_MANAGED_ROOT=.nexo-data/workspaces
NEXO_WORKSPACE_IMPORT_ROOT=
NEXO_WORKSPACE_ARTIFACT_ROOT=.nexo-data/artifacts
```

An empty import root is a security control, not a fallback. Container deployments must mount a
narrow host directory themselves and set `NEXO_CONTAINER_WORKSPACE_IMPORT_ROOT` to that container
path. The base Compose file persists managed Workspace and artifact roots in named volumes, but does
not expose a host directory automatically.

## Authenticated API

All routes use the controller prefix `/api/v1/workspaces`:

| Method and path | Behavior |
|---|---|
| `GET /` | List the authenticated user's registrations. |
| `POST /` | Create an unbound registration. |
| `GET /{workspaceId}` | Read one owned registration. |
| `PUT /{workspaceId}/binding` | Bind managed storage or an import-root-relative mounted project. |
| `DELETE /{workspaceId}` | Delete the registration, never project files; referenced workspaces are rejected. |
| `GET /{workspaceId}/status` | Compare live structure/Git state with the accepted scan. |
| `POST /{workspaceId}/refresh` | Accept and persist the current fingerprint and Git HEAD. |
| `GET /{workspaceId}/tree` | Lazily list one bounded directory page. |
| `GET /{workspaceId}/file` | Return one bounded text-only file excerpt. |

Conversation workspace selection is authoritative server state. The Chat header changes that
selection through the conversation API; the sidebar only links to Workspace management and does not
maintain a competing browser-local active project.

## Spring AI Agent tools

For an Agent request, Nexo resolves the conversation's persisted Workspace and the caller's effective
permission profile before building the callback list. When `WORKSPACE_READ` is allowed and the
Workspace is available, these callbacks join the same request-scoped Spring AI advisor loop used by
plans, Vault retrieval, memory, and MCP:

- `workspace_list_files`
- `workspace_read_file`
- `workspace_search`
- `workspace_git_status`
- `workspace_git_diff`
- `workspace_inspect_project`

The runtime caps the group at 12 calls per request and denies identical repeats. Every call resolves
ownership and availability again, honors cancellation, produces sanitized task evidence, and records
started/completed/denied/failed audit outcomes. Capability questions are answered from the actual
callback snapshot, so a model cannot truthfully claim a Workspace tool that was not attached.

Explicit requests to inspect project files, repository state, or Git diffs are evidence-gated. Model
prose is buffered until a matching successful `workspace_*` execution exists; an invented claim is
not persisted as successful work.

## Read policy and change detection

Directory traversal omits dependency, build, editor, and internal runtime folders such as `.git`,
`node_modules`, `target`, `dist`, and `.nexo-runtime`. Direct reads and search reject environment and
credential files, private keys, certificates/keystores, binary or invalid UTF-8 content, and files
above the configured byte limit. Git support uses only fixed read-only argument arrays; no shell or
model-authored process is started.

A deterministic fingerprint includes sorted relative paths, entry type, size, and modification time,
plus the Git HEAD when present. `CHANGED` warns the user that server project state differs from the
last accepted scan. Refresh records the new baseline; it does not modify project content.

## Deliberately deferred

This delivery does not edit files, run arbitrary commands, mutate Git, upload an existing project,
delegate to workers, or perform approval-gated actions. Those capabilities need explicit write and
command permission families, approval records, bounded execution sandboxes, artifact capture, and
rollback/recovery semantics before they can be attached to a model.
