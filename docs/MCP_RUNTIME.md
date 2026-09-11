# MCP runtime and implementation plan

Nexo IA supports the first governed MCP tool increment through Spring AI 2.0.1. The product keeps
two connection worlds behind one user-owned registry:

1. **Docker MCP Catalog:** reviewed containerized servers reached either through the installed
   Docker MCP CLI over STDIO, through operator-owned SSE Gateway sidecars, or through the Docker MCP
   Toolkit profile configured on the Nexo server machine.
2. **Personal MCP:** a Streamable HTTP endpoint registered by one authenticated user, suitable for a
   server that person built or operates.

The implementation follows Docker's official
[Catalog and Toolkit](https://docs.docker.com/ai/mcp-catalog-and-toolkit/),
[Gateway](https://docs.docker.com/ai/mcp-catalog-and-toolkit/mcp-gateway/), and
[CLI/profile](https://docs.docker.com/ai/mcp-catalog-and-toolkit/cli/) contracts, plus Spring AI's
[MCP client](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html),
[MCP overview](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html), and
[MCP helper](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-helpers.html) references. The
Docker-maintained catalog source remains available in the
[official MCP registry](https://github.com/docker/mcp-registry).

## Implemented plan

### 1. Registry and isolation — complete

- `mcp_connection` stores one user's Docker or remote server registration.
- `mcp_tool_definition` stores the last sanitized tool snapshot and eligibility state. Ordinary
  connections use explicit per-tool selection; the server machine profile is governed as one catalog.
- Every list, mutation, discovery, and runtime lookup starts from the authenticated `user_id`.
- Database foreign keys delete tool snapshots with their connection; no endpoint or tool selection
  can be read through another user's identifier.

### 2. Discovery and transports — complete for the safe first slice

- The backend reads Docker's live catalog for five minutes at a time and falls back to a small
  reviewed free-first list when Docker MCP is unavailable.
- Docker connections use the fixed command `docker mcp gateway run --servers <catalog-id>` without a
  shell when the backend runs where the CLI is available. In the Compose development profile,
  configured server ids resolve instead to isolated SSE sidecars. The server id must
  first resolve through the catalog and match the bounded identifier contract.
- The cross-platform development launchers detect the server machine's Docker MCP Toolkit profile
  and add `compose.mcp-profile.yaml` only when that profile actually exists. The profile Gateway is
  represented in the Hub by the virtual `docker-profile` catalog server and can be inspected like
  every other user-owned connection.
- Personal connections use the official MCP Java SDK's Streamable HTTP transport.
- Discovery initializes the server, paginates a bounded tool list, stores safe metadata and JSON
  input schemas, and preserves still-existing tool selections on refresh. For `docker-profile`, every
  safe discovered tool remains eligible automatically while Gateway control/proxy tools stay excluded.

### 3. Connection and tool enablement — complete

- A newly discovered tool on an ordinary Docker or personal connection is off by default. Its owner
  selects an exact subset and separately enables that connection.
- The Docker machine profile is presented as one persistent catalog: discovery makes all safe tools
  eligible, and the owner enables or disables the catalog once at connection level. There is no
  per-tool picking and no twelve-tool cap for that profile.
- A shared machine profile is infrastructure, not a global permission grant. Connection enablement
  remains isolated per authenticated Nexo user; disabling it removes the whole profile from that
  user's Agent requests without altering the server operator's Docker profile.
- The Hub labels a discovered but disabled connection as **Off in Agent**. Ordinary connections can
  save a changed allow-list; the machine profile instead shows its complete safe catalog and one
  **Enable catalog in Agent** action. Once enabled, the Chat Agent context reports the complete safe
  profile tool count even for snapshots created before profile-level authorization was introduced.
  Discovery alone never appears as active access in Chat.
- The MCP Hub exposes cost, risk hint, setup requirements, health, transport, real tool descriptions,
  and read/destructive/open-world annotations when the server provides them.

### 4. Agent integration — complete for tools

- Chat mode never receives MCP callbacks.
- Agent mode resolves a maximum of four enabled owned connections. Ordinary connections share a
  twelve-selected-tool budget; the machine profile contributes its complete safe discovered catalog.
- Spring AI's `SyncMcpToolCallback` adapts SDK tools into the same governed loop used by Nexo's
  native tools. Up to ten authorized callbacks are attached directly for reliable local-model
  invocation; larger catalogs switch to request-local progressive discovery.
- The Agent capability envelope identifies the exact enabled `mcp_*` callbacks as callable external
  tools, and the composer shows their owned server and tool counts before sending.
- With no enabled MCP callback, the envelope explicitly says that no external MCP tool is connected
  and directs the model to the MCP Hub instead of letting it invent tools or generic capabilities.
  With a matching callback, the model is instructed to call it before claiming external access is
  unavailable.
- Each execution receives a fresh tool index containing only the authenticated owner's enabled
  connection snapshot. `inspect_capabilities` can report safe names and descriptions from that exact
  snapshot, but never connection endpoints, credentials, ownership ids, or disabled connections.
  When the catalog is larger than ten callbacks, Spring AI's `ToolSearchToolCallingAdvisor` performs
  progressive discovery and lets the model select the matching tool instead of injecting every
  schema into the prompt.
- A capability-list question is rendered from the actual callback snapshot without relying on model
  recall. Explicit external research and URL access require at least one recorded `mcp_*` execution
  before answer text is released or the request can complete successfully. Earlier assistant
  refusals are excluded from that forced tool turn so a small local model cannot imitate stale,
  false capability claims.
- For the forced external turn, Nexo exposes only the relevant external family: search/query/find for
  a normal research prompt, or fetch/content/open/url for a concrete URL. This prevents small models
  from selecting a fetch tool with a fabricated URL when a real search tool is available. Only a
  completed MCP call unlocks the final answer; failed and denied calls produce an explicit failure.
- A request may execute at most six MCP calls and at most two calls to one external tool. Repeated
  identical tool arguments are denied.
- Calls honor explicit cancellation, record an argument digest rather than raw input, emit the normal
  tool lifecycle events, persist bounded evidence, and create correlated audit entries.
- External output is capped at 32,000 characters. Exceptions become a controlled tool result and do
  not leak endpoint, credentials, process output, or stack traces to the model.

### 5. Verification — complete for the first slice

- Unit coverage verifies live/fallback catalog parsing, free-first ordering, private endpoint
  policy, secret/config rejection, owned runtime selection, repeated-call denial, and Agent capability
  assembly.
- The complete Java context test applies migration `V27` to PostgreSQL and proves application wiring.
- Frontend schemas reject malformed catalog and connection payloads.

## Runtime flow

```text
authenticated owner
  -> MCP Hub registration
  -> server initialization and bounded tool discovery
  -> ordinary connection: explicit tool selection
     machine profile: complete safe catalog
  -> explicit connection/catalog enablement
  -> Agent request resolves only that owner's enabled connection snapshot
  -> Spring AI progressively discovers the relevant callback
  -> request-owned MCP client and Spring AI callbacks
  -> governed tool call, result, evidence, audit, close
```

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/mcp/catalog` | Live Docker catalog or reviewed fallback |
| `GET` | `/api/v1/mcp/connections` | Current user's connections and tool snapshots |
| `POST` | `/api/v1/mcp/connections/docker` | Register an eligible Docker catalog server |
| `POST` | `/api/v1/mcp/connections/remote` | Register a personal Streamable HTTP endpoint |
| `POST` | `/api/v1/mcp/connections/{id}/discover` | Replace its bounded discovery snapshot |
| `PUT` | `/api/v1/mcp/connections/{id}/tools` | Replace an ordinary connection's explicit allowed-tool subset |
| `PUT` | `/api/v1/mcp/connections/{id}/state` | Enable or disable it for Agent mode |
| `DELETE` | `/api/v1/mcp/connections/{id}` | Remove the owned registration and snapshot |

## Operation

Docker MCP works when the Nexo backend process can execute the Docker CLI with the MCP plugin or an
operator configures a server-id-to-Gateway endpoint. A backend started directly on a developer
workstation can use the locally installed Docker Desktop Toolkit.

The Compose development profile starts two pinned Docker MCP Gateway sidecars: `fetch` and
`duckduckgo`. Each sidecar owns exactly one reviewed server, receives Docker's API socket through
Compose `use_api_socket`, requires a bearer token, has no published host port, and shares only the
dedicated `nexo-mcp` network with the backend. The backend selects those endpoints through
`NEXO_MCP_DOCKER_GATEWAY_ENDPOINTS` and sends `NEXO_MCP_DOCKER_GATEWAY_TOKEN`; the gateway receives
the matching `MCP_GATEWAY_AUTH_TOKEN`. `NEXO_MCP_GATEWAY_TOKEN` can override the development-only
default. The tool containers retain Docker Gateway signature verification, resource limits, and
`no-new-privileges` defaults.

When `docker mcp profile show <profile>` succeeds and its Toolkit database is present, `dev-up.sh`
and `dev-up-windows.ps1` add `compose.mcp-profile.yaml`. That overlay mounts only the server
machine's Docker MCP configuration directory read-only, starts an authenticated `mcp-profile`
Gateway, and registers `docker-profile=http://mcp-profile:8811/sse` with the backend. The default
profile name is `default`; override it with `NEXO_DOCKER_MCP_PROFILE`.

The Gateway can list every server already configured in that machine profile, while Nexo stores an
owner-specific discovered snapshot. The user enables that profile once; all safe discovered tools
then remain in the request-owned catalog and the model chooses the appropriate tool through Spring
AI progressive discovery. Docker Gateway management/proxy tools such as `mcp-add`, `mcp-remove`,
`mcp-exec`, and `code-mode` are excluded because they could bypass Nexo governance. Ordinary profile
tools—including search, time, memory, browser, and Git tools that the Gateway successfully starts—
remain eligible without manual per-tool selection.

Filesystem-backed Docker MCP servers do not inherit arbitrary host access. Configure a narrow
`NEXO_DOCKER_MCP_READ_PATHS` or `NEXO_DOCKER_MCP_WRITE_PATHS` only on the trusted Nexo server when a
profile tool legitimately needs those paths. An empty write allow-list is the safe default.

The sidecars currently use Docker Gateway's SSE transport. The pinned Gateway rejects the
`application/json; charset=utf-8` request media type emitted by the Spring AI-bundled MCP SDK 2.0
Streamable HTTP client, while the official SSE client interoperates successfully. An opt-in Docker
smoke test initializes the real authenticated gateway and verifies `search` and `fetch_content`
discovery so a future dependency upgrade can safely reevaluate Streamable HTTP.

The production backend image deliberately contains no host Docker socket or Docker Desktop
credentials. Production therefore still requires separately isolated, authenticated Gateway
sidecars or the future Companion/broker boundary; the application container never receives the raw
Docker socket.

When neither the CLI nor configured sidecars exist, the MCP Hub renders **Docker runtime
unavailable**, with a normal unavailable cursor and an explanation of the runtime boundary. In the
development profile, Fetch and DuckDuckGo are executable catalog cards. Progress labels and cursors
are reserved for real install, discovery, selection, and enablement requests. Personal Streamable
HTTP registration remains available through **Connect custom** when its endpoint satisfies the
network policy.

Public HTTPS personal endpoints are allowed by default. Loopback and private-network endpoints are
blocked against server-side request forgery. A trusted local development operator may explicitly set
`NEXO_MCP_ALLOW_PRIVATE_ENDPOINTS=true`; this widens which destinations the backend can dereference
and must not be enabled casually on a shared server.

## Deliberately deferred plan

1. Add an encrypted, user-owned Secret Store plus OAuth lifecycle; then enable catalog entries that
   require credentials.
2. Add typed, user-owned Docker configuration rather than reading shared Docker Toolkit settings;
   then enable configuration-dependent catalog entries.
3. Build the signed Nexo Companion/broker so a remote Nexo server can reach Docker MCP running on a
   different endpoint machine. The implemented machine-profile bridge intentionally reads the
   profile of the Nexo server itself; it does not reach a browser user's separate computer.
4. Put write/destructive MCP annotations through the full Permission Engine with previews and fresh
   approval; annotations are untrusted hints, not authorization.
5. Add per-conversation connection selection, resources/prompts, health history, usage counters, and
   reconnect/retry policy.
6. Add a Nexo MCP server authoring template and validator. Arbitrary custom STDIO commands remain
   blocked until executable, working-directory, environment, filesystem, and cancellation policies
   exist.

Free means no Nexo license fee or API key is required by the reviewed server metadata; it never
means Nexo bypasses a provider's account, quota, license, terms, or infrastructure cost. Examples in
Docker Hub include [Fetch](https://hub.docker.com/mcp/server/fetch/tools),
[DuckDuckGo](https://hub.docker.com/mcp/server/duckduckgo/overview),
[Git](https://hub.docker.com/mcp/server/git/config), and
[Playwright](https://hub.docker.com/mcp/server/playwright/overview). Their current requirements are
still re-read from the live catalog before installation.
