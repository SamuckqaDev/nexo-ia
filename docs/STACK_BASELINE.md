# Accepted stack baseline

## Status

**Accepted for release `0.1` scaffolding — 2026-08-16.**

This document is the reproducible implementation baseline for Nexo IA. `TECH_STACK.md` explains the
long-term architecture; this file records what is installed, compiled, run, and tested in the first
release. Patch upgrades require the normal automated test suite and an updated dependency lock or
wrapper. Major and minor line changes require a recorded decision.

## Version matrix

| Area | Accepted baseline | Rule |
|---|---|---|
| Backend language | Java 25 LTS | No preview features |
| Application framework | Spring Boot 4.1.0 | Use the accepted `4.1.x` line |
| AI integration | Spring AI BOM 2.0.0 | Use the accepted `2.0.x` line |
| Build | Maven Wrapper 3.9.16 | Invoke `./mvnw`; Maven 4 is not the baseline |
| Database | PostgreSQL 18 | Pin the container image by explicit major/minor, then test patch upgrades |
| Frontend runtime | Node.js 24 LTS | Frontend tooling only; do not use the EOL Node 23 installation |
| Package manager | npm with committed lockfile | Reproducible installs use `npm ci` |
| UI | React 19.2, TypeScript 6.0, Vite 8.1 | Pin exact resolved versions in the lockfile |
| Server state/navigation | TanStack Query v5 and Router v1 | No second state framework initially |

The selected Spring AI line officially supports Spring Boot 4.0 and 4.1. Java 25 is the current
OpenJDK LTS generation selected for this new project, while Node 24 is an active LTS line. Exact
transitive versions are controlled by the Spring Boot and Spring AI BOMs instead of being repeated
manually.

## Release `0.1` backend

- Spring Web MVC and Server-Sent Events for chat streaming and cancellation.
- Spring Security for authentication, authorization, CSRF protection, and secure session handling.
- Spring Security JOSE for signed access JWTs plus project-owned PostgreSQL session, refresh rotation,
  revocation, and access-monitoring persistence.
- Spring Data JPA for application persistence and Flyway for every schema change.
- PostgreSQL as the only real application database; H2 may be used only for narrow unit tests that do
  not claim PostgreSQL compatibility.
- Spring Validation, Jackson, Actuator, and Micrometer.
- Spring AI Ollama adapter behind a project-owned provider boundary.
- JUnit 5, AssertJ, Mockito, Spring Boot Test, Testcontainers, ArchUnit, and a deterministic fake
  Ollama HTTP service.

Use Spring MVC rather than WebFlux in `0.1`. Ollama streaming is I/O-bound, but SSE does not require
a second reactive programming model. This can be revisited only with profiling evidence.

## Release `0.1` frontend

- React with strict TypeScript.
- Vite and the official React plugin.
- TanStack Router for typed routes and TanStack Query for server state.
- Native `fetch` and an SSE client for HTTP communication.
- Zod at untrusted runtime boundaries, not as a duplicate domain model.
- `styled-components` with a typed Nexo theme and design tokens.
- Axios for ordinary HTTP, React Hook Form with Zod for forms, and Zustand only for shared
  client-owned state; TanStack Query remains the server-state owner.
- Vitest, Testing Library, and Playwright.
- OpenAPI-generated TypeScript contracts from the backend.

The frontend must not retain authentication tokens in `localStorage`. Authentication uses an
`HttpOnly` session cookie; unsafe requests also carry the server-issued CSRF token.

## Authentication and secrets

- Hash passwords with Argon2id through Spring Security's versioned `DelegatingPasswordEncoder`
  format. Begin with OWASP's minimum Argon2id profile of 19 MiB memory, two iterations, and
  parallelism one, then benchmark it on the supported deployment hardware before release.
- Configure session cookies as `HttpOnly`, `SameSite=Lax`, and `Secure` whenever HTTPS is active.
- Rotate the session identifier after authentication, enforce idle and absolute expiry, and support
  per-session and per-user revocation.
- Use Spring Security CSRF protection for browser state-changing requests. `SameSite` is an
  additional defense, not a replacement.
- Apply login throttling without revealing whether an account exists.
- Release `0.1` stores no remote-provider credential: its Ollama endpoint is organization
  configuration, not a secret. Database and bootstrap secrets are injected at runtime and never
  committed or exposed through the UI, logs, prompts, or audit payloads.
- Implement the encrypted application Secret Store before remote providers, MCP credentials, or
  paired devices enter a release.

## Fedora Silverblue development model

The development workstation is Fedora Silverblue. Keep mutable developer toolchains inside a
dedicated current Toolbx container and run containers with rootless Podman on the host:

1. update and reboot the Silverblue host before establishing the reproducible environment;
2. create a dedicated Nexo Toolbx matching the current host generation;
3. install Java 25 and Node 24 LTS in that Toolbx;
4. use the project Maven Wrapper and committed npm lockfile;
5. enable the rootless Podman socket on the host and expose its Docker-compatible socket to
   Testcontainers inside Toolbx;
6. run PostgreSQL 18 as a pinned Podman container;
7. keep Ollama on the host for direct GPU access and configure Nexo with an explicitly allowed host
   endpoint.

Do not layer the complete Java and Node development toolchain into the immutable host. Do not assume
that `/run/host/usr/bin/podman` can be executed as a normal Toolbx binary: the supported integration
is the host service/socket. Document the actual host address used by the backend and test that it is
not exposed beyond the intended interface.

The inspected workstation currently has a Fedora Silverblue 41 host snapshot, a Toolbx based on
Fedora 41, Node 23.7.0 in the current editor environment, host Podman, and host Ollama 0.32.5 with
local models. Node 23 is end-of-life and is explicitly rejected as the project baseline. The host
Ollama service was not running during inspection; model files being present does not prove service
availability.

## Deferred dependencies

| Dependency | Earliest reason to introduce it |
|---|---|
| `pgvector` | Release `0.2` Knowledge/RAG ingestion and retrieval |
| Redis | A benchmark proves a cross-process cache, stream, queue, or latency need |
| MongoDB | No accepted use case; PostgreSQL and JSONB cover the current domain |
| MCP SDK | The MCP phase begins with an isolated integration |
| ComfyUI | Image-generation phase |
| Quartz/workflow engine | Persisted scheduler design no longer meets measured requirements |
| Tauri | The web product and Java lifecycle are stable |
| Kubernetes/microservices | Measured scale or availability requires distribution |

`pgvector` is accepted for RAG but is not installed in release `0.1`. MongoDB and Redis are not
fallback defaults. Adding a datastore creates operational, security, backup, recovery, isolation,
and test obligations and therefore requires evidence.

## Distribution baseline

Backend, frontend, and PostgreSQL are delivered as shared OCI images through Compose. Ollama remains
host-native in the default local profiles. Fedora Silverblue, conventional Linux, Windows, and macOS
receive platform-specific installer, networking, lifecycle, and verification assets without forking
the application. See [Cross-platform build and distribution profiles](DISTRIBUTION_BUILDS.md).

## Scaffold gates

Before application scaffolding is considered ready:

- verify Java 25, Node 24, rootless Podman, PostgreSQL, and host Ollama connectivity from the dedicated
  Toolbx;
- generate the Maven and npm wrappers/lockfiles and enforce runtime versions;
- create a minimal threat model and define the release-blocking vulnerability policy;
- benchmark the selected Argon2id parameters;
- prove a PostgreSQL Testcontainers test and an opt-in Ollama health/model-discovery test;
- record the exact container image digests used by CI and local integration tests.

## Primary references

- [OpenJDK 25](https://openjdk.org/projects/jdk/25/)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring AI getting started](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Apache Maven download](https://maven.apache.org/download.cgi)
- [Node.js releases](https://nodejs.org/en/about/previous-releases)
- [Vite releases](https://vite.dev/releases)
- [PostgreSQL versioning policy](https://www.postgresql.org/support/versioning/)
- [Spring Security JWT resource-server support](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [Fedora Silverblue Toolbx](https://docs.fedoraproject.org/en-US/fedora-silverblue/toolbox/)
- [Testcontainers with Podman](https://java.testcontainers.org/supported_docker_environment/)
