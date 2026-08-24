# MCP runtime and implementation plan

Nexo IA supports the first governed MCP tool increment through Spring AI 2.0.1. The product keeps
two connection worlds behind one user-owned registry:

1. **Docker MCP Catalog:** reviewed containerized servers discovered through the installed Docker
   MCP CLI and launched through the Docker MCP Gateway's STDIO transport.
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
- `mcp_tool_definition` stores the last sanitized tool snapshot and explicit enabled state.
- Every list, mutation, discovery, and runtime lookup starts from the authenticated `user_id`.
- Database foreign keys delete tool snapshots with their connection; no endpoint or tool selection
  can be read through another user's identifier.

### 2. Discovery and transports — complete for the safe first slice

- The backend reads Docker's live catalog for five minutes at a time and falls back to a small
  reviewed free-first list when Docker MCP is unavailable.
- Docker connections use the fixed command `docker mcp gateway run --servers <catalog-id>` without a
  shell. The server id must first resolve through the catalog and match the bounded identifier
  contract.
- Personal connections use the official MCP Java SDK's Streamable HTTP transport.
- Discovery initializes the server, paginates a bounded tool list, stores safe metadata and JSON
  input schemas, and preserves still-existing tool selections on refresh.

### 3. Explicit enablement — complete

- A newly discovered tool is off by default.
- The owner selects an exact subset of discovered external names and separately enables the
  connection.
- The MCP Hub exposes cost, risk hint, setup requirements, health, transport, real tool descriptions,
  and read/destructive/open-world annotations when the server provides them.

### 4. Agent integration — complete for tools

- Chat mode never receives MCP callbacks.
- Agent mode resolves a maximum of four enabled owned connections and twelve selected tools.
- Spring AI's `SyncMcpToolCallback` adapts SDK tools into the same `ToolCallingAdvisor` loop used by
  Nexo's `update_plan` and `search_knowledge` tools.
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
  -> explicit tool selection
  -> explicit connection enablement
  -> Agent request resolves only that owner's enabled snapshot
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
| `PUT` | `/api/v1/mcp/connections/{id}/tools` | Replace the explicit allowed-tool subset |
| `PUT` | `/api/v1/mcp/connections/{id}/state` | Enable or disable it for Agent mode |
| `DELETE` | `/api/v1/mcp/connections/{id}` | Remove the owned registration and snapshot |

## Operation

Docker MCP works when the Nexo backend process can execute the Docker CLI with the MCP plugin. A
backend started directly on a developer workstation can use the locally installed Docker Desktop
Toolkit. The production backend image deliberately contains no host Docker socket or Docker Desktop
credentials, so its catalog reports unavailable until a separately authenticated Companion/broker
boundary is implemented; mounting the host socket into the application container is not accepted as
an implicit shortcut.

Public HTTPS personal endpoints are allowed by default. Loopback and private-network endpoints are
blocked against server-side request forgery. A trusted local development operator may explicitly set
`NEXO_MCP_ALLOW_PRIVATE_ENDPOINTS=true`; this widens which destinations the backend can dereference
and must not be enabled casually on a shared server.

## Deliberately deferred plan

1. Add an encrypted, user-owned Secret Store plus OAuth lifecycle; then enable catalog entries that
   require credentials.
2. Add typed, user-owned Docker configuration rather than reading shared Docker Toolkit settings;
   then enable configuration-dependent catalog entries.
3. Build the signed Nexo Companion/broker so a containerized or remote server can reach a user's
   local Docker MCP Gateway without receiving the host Docker socket.
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
