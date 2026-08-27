# Cross-platform build and distribution profiles

## Status

**Development bootstrap and unsigned Electron Companion foundation implemented; signed release
packaging remains planned.**

Nexo IA will publish four supported distribution profiles from the same source and version:

1. Fedora Silverblue;
2. conventional Linux distributions;
3. Windows;
4. macOS.

These are packaging and installation profiles, not four forks of the application. The backend,
frontend, database migrations, HTTP contracts, container images, and browser experience remain the
same. Platform-specific files configure the container engine, host Ollama connection, persistent
directories, lifecycle commands, and—later—the native Nexo Companion.

## Common release artifacts

Every platform release uses the same immutable, versioned application artifacts:

```text
nexo-ia-<version>/
  compose.yaml
  compose.profiles.yaml
  .env.example
  checksums.txt
  licenses/
  config/
  install/
    silverblue/
    linux/
    windows/
    macos/
```

The Compose application contains:

- `nexo-backend`: Java 25/Spring Boot application;
- `nexo-frontend`: compiled static React application and web gateway;
- `nexo-postgres`: PostgreSQL 18 with a persistent named volume;
- optional future profiles such as observability and production-selected MCP servers.

Ollama is external to the default Compose application. Nexo connects to a configurable host endpoint.
Containerizing Ollama remains an optional server/DGX profile when GPU runtime support is deliberately
configured and tested.

ComfyUI is also host-local in the development bootstrap so it can use the platform's GPU runtime.
`scripts/setup-macos.sh`, `scripts/setup-linux.sh`, and `scripts/setup-windows.ps1` install the
development dependencies, Ollama models, official ComfyUI checkout, and a default local checkpoint.
They then delegate to the shared Compose startup. These are repeatable developer installers, not yet
signed production packages, updaters, backup tools, or uninstallers.

## Profile matrix

| Profile | Container engine | Ollama | User entry point | Native integration |
|---|---|---|---|---|
| Fedora Silverblue | Rootless Podman + Compose provider | Host service | Browser or Electron | Unsigned Electron foundation |
| Conventional Linux | Docker Engine Compose or rootless Podman | Host service | Browser or Electron | Unsigned Electron foundation |
| Windows | Docker Desktop with WSL 2 backend | Windows host service | Browser or Electron | Unsigned Electron foundation |
| macOS | Docker Desktop or supported Podman machine | macOS host service | Browser or Electron | Unsigned Electron foundation |

The server remains web-first. The optional `desktop/` Electron application now supplies the first
authorized local-filesystem capability without placing Node APIs in React. Its preload exposes only
pairing, state, and folder-selection methods; the renderer remains sandboxed with context isolation
and no Node integration. Current packages are development/unsigned artifacts, not trusted public
installers. A browser alone never receives host privileges.

Development verification:

```bash
./scripts/dev-electron.sh

# Or verify each layer manually
cd frontend && npm run build
cd desktop && npm install && npm run typecheck && npm test && npm run build
NEXO_RENDERER_URL=http://127.0.0.1:5173 npm run dev
```

`scripts/dev-electron.sh` is the macOS/Linux development launcher. By default it starts the shared
Compose stack, waits for the Vite renderer, installs missing desktop dependencies, builds the
Electron runtime, and keeps Electron in the foreground. `--skip-stack` reuses a running local stack;
`--renderer-url http://<server-ip>:5173` loads a trusted remote development renderer without starting
local containers; and `--dry-run` validates the launcher configuration without opening a window.
The shared startup reuses healthy containers, retries failed core services without deleting named
volumes, and retries each Docker MCP Gateway sidecar three times. MCP catalog availability is not a
core startup dependency: when Docker's remote catalog cannot be downloaded, the application starts
in a reported degraded mode and a later launcher run retries the optional sidecars. Conflicting
backend, frontend, PostgreSQL, or Mailpit development ports are moved to the next free local port and
persisted in the private `.env`; the launcher then reads the effective frontend port before opening
Electron. It never stops a container or process that belongs to another project.
Plain HTTP remains limited to a trusted development network.

The packaged static renderer still requires a production server-origin/bootstrap contract before it
is a supported distributable. Code signing, notarization, automatic updates, credential rotation,
and real-host installation tests remain release gates.

## Fedora Silverblue profile

This is the creator's primary development and reference installation.

- Use rootless Podman on the immutable host.
- Use `podman compose` with the shared Compose definition.
- Keep Java, Maven, Node, and development utilities inside a dedicated Toolbx; production execution
  does not require that development Toolbx.
- Run Ollama directly on the host for GPU access.
- Use `host.containers.internal` only after connectivity is verified for the installed Podman version;
  permit an explicit configured gateway address when required.
- Provide user-level systemd/Quadlet integration after the Compose installation is stable, enabling
  start, stop, restart, health inspection, and controlled startup without root.
- Store data under explicit user-owned application directories or named volumes, never inside a
  transient Toolbx.

Planned installer assets:

```text
install/silverblue/
  README.md
  install.sh
  uninstall.sh
  nexo-ia.container-or-compose-unit
  verify-host.sh
```

The installer must preview its actions, detect rather than silently install host prerequisites, and
preserve PostgreSQL data during ordinary upgrades or uninstallation unless deletion is separately
confirmed.

## Conventional Linux profile

The conventional Linux package supports maintained x86-64 and ARM64 distributions through the same
OCI images.

- Prefer Docker Compose for the general instructions and document rootless Podman as a supported
  alternative after both paths pass CI and real-host tests.
- Detect the Compose command rather than assuming legacy `docker-compose`.
- Keep Ollama on the host by default and configure the host gateway explicitly.
- Provide a systemd user service when rootless operation supports it; a system service is an explicit
  administrator choice.
- Avoid distribution-specific `.deb` or `.rpm` packages for the server until they offer real value
  beyond the Compose bundle.

Planned installer assets:

```text
install/linux/
  README.md
  install.sh
  uninstall.sh
  nexo-ia.service
  verify-host.sh
```

## Windows profile

- Use Docker Desktop with its WSL 2 backend for the containerized server, frontend, and PostgreSQL.
- Run Ollama natively on Windows and connect from the backend container through
  `host.docker.internal`.
- Provide signed PowerShell installation, verification, update, backup, and uninstall scripts.
- Store configuration and user-visible exports in an appropriate per-user Windows application data
  directory; keep PostgreSQL in a managed container volume.
- Do not require Administrator privileges for ordinary lifecycle commands. Clearly identify any
  prerequisite installation that does require elevation.
- Later publish the Nexo Companion as a signed native package/service with explicit Windows
  permissions; do not mount the complete Windows filesystem into the backend container.

Planned installer assets:

```text
install/windows/
  README.md
  Install-Nexo.ps1
  Update-Nexo.ps1
  Backup-Nexo.ps1
  Uninstall-Nexo.ps1
  Test-NexoHost.ps1
```

## macOS profile

- Support Docker Desktop first; evaluate Podman machine as an additional tested engine rather than
  assuming equivalent host networking behavior.
- Run Ollama natively on macOS so Apple Silicon acceleration remains managed by the host application.
- Connect from Docker through `host.docker.internal`; configure and verify the equivalent gateway for
  Podman machine.
- Publish universal installer scripts for Apple Silicon and Intel only while every included image and
  native artifact actually supports both architectures.
- Later sign and notarize the Nexo Companion and request macOS Automation, Accessibility, filesystem,
  or notification permissions only when the corresponding capability is invoked.

Planned installer assets:

```text
install/macos/
  README.md
  install.sh
  update.sh
  backup.sh
  uninstall.sh
  verify-host.sh
```

## Configuration and networking

All profiles expose the same documented environment contract. At minimum it contains:

- public Nexo listen address and port;
- PostgreSQL database, user, password-file reference, and volume;
- Ollama base URL, timeout, allowed models, and health-check policy;
- bootstrap-secret file reference;
- data, artifact, backup, and log locations;
- secure-cookie and trusted-proxy configuration.

Default local installation binds the Nexo web gateway to loopback. Private-network exposure is an
explicit configuration with authenticated bootstrap, secure cookies, HTTPS or a trusted TLS proxy,
allowed origins, firewall guidance, and audit. PostgreSQL is never published to the host network by
default. The backend reaches it only through the private Compose network.

The implemented opt-in is `NEXO_BIND_ADDRESS=0.0.0.0`. It publishes only the Nexo web/backend
development entry points selected by the Compose profile; PostgreSQL and Mailpit remain loopback
bound. The Nginx web gateway supports the authenticated `nexo.runtime.v1` WebSocket upgrade used by
the paired Companion. On a trusted development LAN, the unsigned Desktop runtime may load
`NEXO_RENDERER_URL=http://<server-ip>:5173`; an Internet-facing deployment must terminate TLS and
use HTTPS/WSS plus secure cookies before pairing devices.

## Images and architectures

- Publish OCI images for `linux/amd64` and `linux/arm64` when both pass the complete release suite.
- Use multi-stage builds and a minimal supported Java runtime image for the backend.
- Pin every base image by digest in release automation and produce an SBOM and checksums.
- Sign release images and native installers before they are described as trusted production builds.
- Build once per commit and promote the same tested image digest across supported profiles.
- Keep database data and user artifacts outside immutable images.

Windows and macOS run the Linux server containers through their container virtual-machine layer. The
later Companion is native because it must interact with the actual endpoint operating system.

## Release verification matrix

No profile is called supported until CI and real-host validation prove:

- fresh install, bootstrap, login, streaming chat, cancellation, and logout;
- restart without data loss;
- backup and restore into the same supported release;
- upgrade from the previous supported version;
- Ollama discovery and inference through the platform's host gateway;
- secure default bindings and no published PostgreSQL port;
- two-user isolation and session revocation;
- clean stop and uninstall with an explicit keep/delete-data choice;
- x86-64 and ARM64 behavior for every advertised architecture.

Hardware-accelerated Ollama tests are platform-specific and opt-in in ordinary development, but are
required on representative release machines before claiming GPU support.

## Delivery order

1. Build and validate the shared OCI images and Compose file.
2. Deliver Fedora Silverblue as the reference installation.
3. Validate conventional Linux with Docker and Podman.
4. Validate Windows with Docker Desktop/WSL 2.
5. Validate macOS on Apple Silicon, then Intel only if the project commits to supporting it.
6. Add signed native Companion packages when endpoint-control capabilities begin.

This order affects implementation sequence, not product importance. Platform-specific behavior must
remain isolated in installer and Companion adapters rather than entering the Nexo domain core.
