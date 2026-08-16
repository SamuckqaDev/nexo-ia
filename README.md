# Nexo IA

Nexo IA is a local artificial intelligence assistant created to provide a practical and progressive
way to learn how LLMs, RAG, tools, MCP, memory, agents, and permission control work.

The project will be built from scratch through small releases, documented decisions, and tests. The
goal is not only to produce a working assistant, but also to understand every component and explain
why it exists.

## Current status

The project is in its foundation phase. We are defining its identity, purpose, scope, and roadmap
before selecting and implementing the architecture.

## Documentation

- [Bilingual product and identity presentation](docs/site/index.html) — English and Portuguese selected in the page.
- [Identity](docs/IDENTITY.md)
- [Goals](docs/GOALS.md)
- [Product vision](docs/PRODUCT_VISION.md)
- [Feature catalog](docs/FEATURES.md)
- [Knowledge Vaults](docs/KNOWLEDGE_VAULTS.md)
- [Context isolation and Skill governance](docs/CONTEXT_AND_SKILL_GOVERNANCE.md)
- [Governed project database access](docs/DATABASE_ACCESS.md)
- [Agent capabilities](docs/AGENT_CAPABILITIES.md)
- [Execution plans](docs/EXECUTION_PLANS.md)
- [Enterprise architecture](docs/ENTERPRISE_ARCHITECTURE.md)
- [Device management and execution audit](docs/DEVICE_MANAGEMENT.md)
- [Technology stack](docs/TECH_STACK.md)
- [Principles](docs/PRINCIPLES.md)
- [Scope](docs/SCOPE.md)
- [Learning roadmap](docs/ROADMAP.md)
- [Project decisions](docs/DECISIONS.md)

## Learning pills

Small, source-backed lessons discovered during development live in [pills](pills/README.md). A pill
captures one finding, its evidence, and its concrete impact on Nexo IA.

## Documentation site

The visual site includes a local Markdown documentation portal. Markdown files in `docs/` remain the
source of truth. After adding or changing a document, rebuild the browser data bundle with:

```bash
node scripts/build-docs-data.mjs
```

Then serve the repository through a local HTTP server or open `docs/index.html`; the generated bundle
keeps document rendering available without a CDN or internet connection.
