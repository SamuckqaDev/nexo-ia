# Nexo IA

Nexo IA is a local artificial intelligence assistant created to provide a practical and progressive
way to learn how LLMs, RAG, tools, MCP, memory, agents, and permission control work.

The project will be built from scratch through small releases, documented decisions, and tests. The
goal is not only to produce a working assistant, but also to understand every component and explain
why it exists.

## Current status

The product foundation and release `0.1` scope are documented. Implementation has started on the
`feat/project-scaffold` branch with a Spring Boot backend, React frontend, PostgreSQL Compose service,
and the first backend-to-frontend system contract. See
[Implementation status](docs/IMPLEMENTATION_STATUS.md) for verified and intentionally pending work.

## Documentation

- [Bilingual product and identity presentation](docs/site/index.html) — English and Portuguese selected in the page.
- [Identity](docs/IDENTITY.md)
- [Goals](docs/GOALS.md)
- [Product vision](docs/PRODUCT_VISION.md)
- [Feature catalog](docs/FEATURES.md)
- [Knowledge Vaults](docs/KNOWLEDGE_VAULTS.md)
- [Context isolation and Skill governance](docs/CONTEXT_AND_SKILL_GOVERNANCE.md)
- [Governed project database access](docs/DATABASE_ACCESS.md)
- [MVP and release strategy](docs/MVP_AND_RELEASE_STRATEGY.md)
- [Agent capabilities](docs/AGENT_CAPABILITIES.md)
- [Execution plans](docs/EXECUTION_PLANS.md)
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

Start or recreate the complete development environment with:

```bash
./scripts/dev-up.sh
```

The script detects Docker Compose or Podman Compose, creates a private `.env` with random local
secrets when absent, removes only previous Nexo IA containers, and preserves named volumes. It then
builds the backend image and starts PostgreSQL, Mailpit, and the React frontend through
`npm run dev` on Node.js 24. The default addresses are frontend `http://127.0.0.1:5173`, backend
`http://127.0.0.1:8080`, and Mailpit `http://127.0.0.1:8025`.

## Documentation site

The visual site includes a local Markdown documentation portal. Markdown files in `docs/` remain the
source of truth. After adding or changing a document, rebuild the browser data bundle with:

```bash
node scripts/build-docs-data.mjs
```

Then serve the repository through a local HTTP server or open `docs/index.html`; the generated bundle
keeps document rendering available without a CDN or internet connection.
