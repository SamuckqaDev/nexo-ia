# Nexo IA

Nexo IA is a local artificial intelligence assistant created to provide a practical and progressive
way to learn how LLMs, RAG, tools, MCP, memory, agents, and permission control work.

The project will be built from scratch through small releases, documented decisions, and tests. The
goal is not only to produce a working assistant, but also to understand every component and explain
why it exists.

## Current status

The product foundation and release `0.1` scope are documented. Implementation is on the
`feat/project-scaffold` branch: local identity and sessions, the user-scoped Provider Registry, and
private conversations that stream answers from a local Ollama model over SSE with cancellation and
per-message token and latency accounting. See
[Implementation status](docs/IMPLEMENTATION_STATUS.md) for verified and intentionally pending work.

## Prerequisites

- Java 25 LTS — the backend does not compile on an earlier JDK. On macOS:
  `brew install openjdk@25` and export `JAVA_HOME` accordingly.
- Node.js 24 LTS.
- Docker or Podman with Compose.
- Ollama on the host with at least one installed chat model, for real inference.

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

Tests tagged `ollama` need a real installed model and stay out of the default suite. Run them
deliberately against a live installation:

```bash
cd backend && ./mvnw test -Dexcluded.test.groups= -Dgroups=ollama
```

Override `NEXO_SMOKE_OLLAMA_URL` and `NEXO_SMOKE_OLLAMA_MODEL` when your endpoint or model differs.

## Documentation

- [Bilingual product and identity presentation](docs/site/index.html) — English and Portuguese selected in the page.
- [Identity](docs/IDENTITY.md)
- [Goals](docs/GOALS.md)
- [Product vision](docs/PRODUCT_VISION.md)
- [Feature catalog](docs/FEATURES.md)
- [Knowledge Vaults](docs/KNOWLEDGE_VAULTS.md)
- [RAG and retrieval architecture](docs/RAG_ARCHITECTURE.md)
- [Context isolation and Skill governance](docs/CONTEXT_AND_SKILL_GOVERNANCE.md)
- [Governed project database access](docs/DATABASE_ACCESS.md)
- [MVP and release strategy](docs/MVP_AND_RELEASE_STRATEGY.md)
- [Agent capabilities](docs/AGENT_CAPABILITIES.md)
- [Spring AI Agent runtime](docs/SPRING_AI_AGENT_RUNTIME.md)
- [MCP runtime and implementation plan](docs/MCP_RUNTIME.md)
- [Execution plans](docs/EXECUTION_PLANS.md)
- [Local image generation](docs/IMAGE_GENERATION.md)
- [Enterprise architecture](docs/ENTERPRISE_ARCHITECTURE.md)
- [Device management and execution audit](docs/DEVICE_MANAGEMENT.md)
- [Technology stack](docs/TECH_STACK.md)
- [Accepted stack baseline](docs/STACK_BASELINE.md)
- [Cross-platform build and distribution profiles](docs/DISTRIBUTION_BUILDS.md)
- [Implementation status](docs/IMPLEMENTATION_STATUS.md)
- [Principles](docs/PRINCIPLES.md)
- [Scope](docs/SCOPE.md)
- [Learning roadmap](docs/ROADMAP.md)
- [Project decisions](docs/DECISIONS.md)

## Learning pills

Small, source-backed lessons discovered during development live in [pills](pills/README.md). A pill
captures one finding, its evidence, and its concrete impact on Nexo IA.

## Development startup

On a new workstation, use the platform bootstrap that installs the host prerequisites, pulls the
default Ollama models, installs the official local ComfyUI runtime and checkpoint, and starts Nexo:

```bash
# macOS
./scripts/setup-macos.sh

# conventional Linux (apt, dnf, or pacman)
./scripts/setup-linux.sh
```

```powershell
# Windows PowerShell
.\scripts\setup-windows.ps1
```

The image checkpoint is roughly 2 GB. Set `NEXO_SKIP_IMAGE_RUNTIME=1` on macOS/Linux or pass
`-SkipImageRuntime` on Windows when image generation is intentionally not required.

Start or recreate the complete development environment with:

```bash
./scripts/dev-up.sh
```

The script detects Docker Compose or Podman Compose, creates a private `.env` with random local
secrets when absent, removes only previous Nexo IA containers, and preserves named volumes. It then
builds the backend image and starts PostgreSQL, Mailpit, and the React frontend through
`npm run dev` on Node.js 24. The default addresses are frontend `http://127.0.0.1:5173`, backend
`http://127.0.0.1:8080`, and Mailpit `http://127.0.0.1:8025`. With Docker Compose, development also
starts authenticated, non-published Docker MCP Gateway sidecars for the free Fetch and DuckDuckGo
catalog servers. Set `NEXO_MCP_GATEWAY_TOKEN` to override the local-only default gateway token.
When ComfyUI is running on the host, the backend reaches it through
`NEXO_CONTAINER_COMFYUI_BASE_URL` (default `http://host.containers.internal:8188`).

## Documentation site

The visual site includes a local Markdown documentation portal. Markdown files in `docs/` remain the
source of truth. After adding or changing a document, rebuild the browser data bundle with:

```bash
node scripts/build-docs-data.mjs
```

Then serve the repository through a local HTTP server or open `docs/index.html`; the generated bundle
keeps document rendering available without a CDN or internet connection.
