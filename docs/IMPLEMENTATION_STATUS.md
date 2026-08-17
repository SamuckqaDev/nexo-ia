# Implementation status

## Current increment

**Increment 1 — local authentication foundation.**

The first implementation branch is `feat/project-scaffold`. It establishes the source layout and a
minimal vertical connection plus the first release `0.1` identity slice.

## Added

- Java 25 Spring Boot backend definition with the accepted Spring Boot and Spring AI BOM lines.
- Public, minimal `GET /api/v1/system` identity endpoint.
- Spring Security default-deny boundary with only system identity and health permitted anonymously.
- PostgreSQL, Flyway, JPA, and revocable token-session configuration.
- First foundation migration.
- React and TypeScript frontend shell that probes the backend system endpoint.
- Frontend behavior test and backend MVC security/contract test.
- Project-local `build-nexo` Skill containing the accepted code standards.
- Standard backend flow using a thin documented controller, service, separate response record,
  `ResponseEntity`, `BaseResponse`, and global personalized exception handling.
- Modular frontend system-status flow using a page, component, hook, TanStack Query, Axios, Zod,
  `styled-components`, typed theme, and centralized API error normalization.
- Private PostgreSQL base Compose service with no published database port.
- PostgreSQL 18.4 volume mounted at `/var/lib/postgresql`, matching the official image's versioned
  `PGDATA` layout introduced in PostgreSQL 18.
- Development-only Compose override that binds PostgreSQL exclusively to `127.0.0.1:5432` for a
  backend running outside the container network.
- Environment-variable contract for PostgreSQL, Ollama, server binding, and secure cookies.
- Atomic first-Owner bootstrap status and creation endpoints, protected by a database-level
  single-Owner constraint.
- Separate `user_account` and `password_credential` persistence with normalized identities and
  PBKDF2 adaptive password hashes.
- Username-or-email login, current-profile lookup, short-lived access JWTs in the `NEXO_ACCESS`
  `HttpOnly` cookie, and opaque refresh tokens in the narrower `NEXO_REFRESH` cookie.
- Server-side session and current-`jti` validation on every protected access, refresh-token hash
  storage, pessimistically locked rotation, replay detection, immediate compromise revocation, and
  logout cookie clearing.
- Active-session and recent-access endpoints with initial/last IP, user-agent, timestamps, expiry,
  outcome, and event type. Forwarded addresses remain untrusted until a deployment configures a
  trusted proxy boundary.
- SPA CSRF cookie/header flow and standardized JSON responses for unauthenticated and denied
  requests.
- React first-run Owner form, login form, authenticated profile state, and logout flow using Axios,
  TanStack Query, React Hook Form, and Zod.
- Password recovery and authenticated password change with reset-token expiry, password reuse
  protection, and revocation of other sessions.
- User-visible session and device management with current-session identification, remote revocation,
  ownership isolation, and revocation audit events.
- Persistent login-attempt history using hashed normalized identifiers and IP addresses, with
  configurable progressive lock periods after 5, 7, and 10 consecutive failures.
- Owner-only Member administration with creation, listing, activation, and disablement; disabling
  a Member immediately revokes every active session and records the administrative event.
- Self-service revocation of every other active session while preserving the current session.
- Owner-authorized inspection and individual revocation of Member sessions with target ownership
  validation and administrative audit events.
- Responsive application shell with a collapsible, feature-only branded sidebar, contextual header,
  product dashboard, and mapped navigation for Chat, Cowork, tasks, Vaults, and Skills. Account
  actions, profile photo, Settings, and Administration live exclusively in the header account menu.
- Persistent light and dark visual themes derived from the Nexo IA color system, with system-theme
  preference as the initial default and an accessible header toggle.
- Responsive navigation moves into an accessible hamburger menu in the header on compact screens.
  Desktop navigation uses a branded workspace panel, an emphasized active-item rail, and a dedicated
  panel control at the bottom instead of a floating arrow. Header controls, account trigger, avatar
  frame, menus, and navigation items share the same border and control-radius tokens.
- Brand color semantics are reflected in the interface: cyan identifies Nexo capabilities and
  processing, while coral highlights the authenticated person, decisions, notifications, planned
  states, and secondary navigation accents.
- Settings now groups Profile, Security and sessions, Providers, and Token usage. Profile photo and
  account identity are centralized there; security reuses the implemented password and device
  controls, while provider configuration and usage accounting remain visibly marked as planned
  until their backend contracts exist.
- Twenty-eight passing backend tests and fourteen passing frontend tests. The authentication
  flow was also verified against a disposable PostgreSQL 18.4 instance: migrations, bootstrap,
  login, authenticated profile, and logout.

## Intentionally incomplete

- Maven Wrapper scripts and JAR have not been generated because Java/Maven are unavailable in the
  preserved workstation environment.
- Backend/frontend production container images are not added until their builds can be executed and
  tested rather than written speculatively.
- The official Skill validator cannot run in the preserved environment because its `PyYAML`
  dependency is absent. Equivalent frontmatter, naming, key, and length checks pass through Node;
  rerun the official validator when the dependency is available.
- The base Compose service currently starts PostgreSQL only. Backend and frontend remain development
  processes until their container images pass local verification. Development uses
  `compose.dev.yaml`; production must not apply that database-port override.
- Organization membership beyond installation-level Owner/Member roles remains a subsequent
  identity increment.
- Ollama discovery, conversation, SSE, usage, and audit remain subsequent release `0.1` increments.

## Next verification

Next delivery checks:

1. generate and verify Maven Wrapper 3.9.16;
2. add tested multi-stage backend and frontend container images to Compose.
