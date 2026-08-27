# Server and local-device workspaces

Nexo project execution starts from an owner-scoped Workspace record on the server. Its content may
live in server storage or in a folder bound through Nexo Desktop. The browser selects the persisted
Workspace and optional device binding for one conversation; it never sends a local folder handle,
an absolute filesystem path, or a command for the model to execute. Authorization, orchestration,
plans, task evidence, and audit remain on the Nexo server.

## Storage modes

| Mode | Resolution | First-release rule |
|---|---|---|
| `MANAGED` | `{managed-root}/{owner-id}/{workspace-id}` | Nexo creates and owns the directory. |
| `MOUNTED` | `{import-root}/{relativePath}` | Existing server project; Owner only; disabled when the import root is empty. |
| `UNBOUND` | no filesystem path | Metadata may exist, but no file or Agent tool is available. |

An `UNBOUND` Workspace can additionally have one or more `workspace_binding` records owned through a
paired device. Each record contains the Workspace id, device id, opaque Desktop-local binding id,
display metadata, status, fingerprint, and Git metadata. It never contains the absolute device path.
When that binding is selected on a Conversation and online, the same six Spring AI tools are proxied
over the authenticated `nexo.runtime.v1` channel to Nexo Desktop.

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
| `GET /{workspaceId}/bindings` | List owned Desktop bindings and their effective online status. |
| `GET /{workspaceId}/bindings/{bindingId}/tree` | Lazily list a local directory through its online Desktop. |

The device-authenticated runtime routes use the controller prefix `/api/v1/device-runtime`:

| Method and path | Behavior |
|---|---|
| `POST /pair` | Consume a short-lived browser-created pairing code and return the credential once. |
| `GET /connect` (WebSocket upgrade) | Open the authenticated outbound runtime channel. |
| `POST /workspaces/{workspaceId}/bindings` | Register or refresh opaque binding metadata. |

Conversation workspace selection is authoritative server state. A new Chat shows an explicit
Workspace setup card before the first message: the user may choose **No workspace**, select an
existing registration, or open a local project folder. `POST /api/v1/conversations` accepts the
optional selected `workspaceId`; the backend authorizes it, resolves the preferred available local
binding, and persists both with the new conversation in one transaction. The first model request can
therefore never run between conversation creation and Workspace attachment. Existing conversations
change or clear the selection through `PUT /api/v1/conversations/{conversationId}/workspace` in the
Chat header. The sidebar does not maintain a competing browser-local active project.

In Nexo Desktop, the folder-plus action beside the Chat workspace selector is the primary local
project entry point. One click opens the operating system's native directory chooser: Finder on
macOS, Explorer on Windows, and the configured desktop chooser on Linux. Electron returns only an
opaque, five-minute selection id and the folder's display name to React. After the user confirms a
folder, the authenticated frontend creates an `UNBOUND` Workspace, pairs the Desktop automatically
when required, and asks Electron to consume the one-time selection while registering the device
binding. The conversation then persists that Workspace and the backend resolves its preferred
available binding. Cancellation creates no registration; a binding failure removes the empty
registration. The native absolute path remains only in Electron's encrypted runtime store.

The Projects page exposes that native chooser as its primary **Open project folder** action. It does
not ask for a path or name: the confirmed directory supplies the display name and the one-time native
selection supplies the binding. Server-managed creation remains a separate secondary action and also
does not accept a local path. `MOUNTED` remains a backend deployment capability for an explicitly
configured server import root; it is not presented as the normal local-project workflow.

The native bridge depends on a sandbox-compatible CommonJS preload at
`desktop/dist/preload/index.cjs`. The desktop build validates that artifact, and Chat reports an
unavailable native picker instead of redirecting silently when the bridge is absent. Restart the
Electron process after rebuilding so the BrowserWindow loads the current preload.

## Spring AI Agent tools

For an Agent request, Nexo resolves the conversation's persisted Workspace, optional local binding,
and the caller's effective
permission profile before building the callback list. When `WORKSPACE_READ` is allowed and the
Workspace is available, these callbacks join the same request-scoped Spring AI advisor loop used by
plans, Vault retrieval, memory, and MCP:

- `workspace_list_files`
- `workspace_read_file`
- `workspace_search`
- `workspace_git_status`
- `workspace_git_diff`
- `workspace_inspect_project`

The runtime caps the group at 12 calls per request and denies identical repeats. Server bindings run
inside the backend; local bindings are dispatched to the exact authenticated device and opaque
Desktop binding selected by the conversation. Every call resolves
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

A deterministic fingerprint includes sorted relative paths, size, and modification time for local
bindings and includes entry type plus Git HEAD for server bindings. The Desktop refreshes its
fingerprint at connection time and every minute. A changed fingerprint marks the binding `CHANGED`;
a missing folder marks it `MISSING`. The frontend polls binding status and warns before the Agent
continues to rely on stale structure metadata.

For server bindings, the fingerprint
includes sorted relative paths, entry type, size, and modification time,
plus the Git HEAD when present. `CHANGED` warns the user that server project state differs from the
last accepted scan. Refresh records the new baseline; it does not modify project content.

## Deliberately deferred

This delivery does not edit files, run arbitrary commands, mutate Git, copy a device project to the
server, delegate to workers, or perform approval-gated actions. Those capabilities need explicit write and
command permission families, approval records, bounded execution sandboxes, artifact capture, and
rollback/recovery semantics before they can be attached to a model.
