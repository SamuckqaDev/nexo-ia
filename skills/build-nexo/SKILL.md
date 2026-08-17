---
name: build-nexo
description: Apply the creator's architecture, Java, Spring, API, data-flow, frontend, testing, and delivery standards when implementing, refactoring, or reviewing Nexo IA. Use for any change to Nexo application code, modules, DTOs, services, controllers, repositories, exceptions, frontend features, tests, or project structure.
---

# Build Nexo

## Workflow

1. Inspect the requested module and nearby conventions before editing.
2. Identify the smallest business-oriented module or submodule that owns the change.
3. Prefer the library's official contract when it genuinely conflicts with a project convention.
4. Keep controllers free of business logic and place functionality in services.
5. Use separate records for DTOs and personalized exceptions for application failures.
6. Reuse established generic infrastructure when repetition is material; avoid abstractions for
   trivial, one-off, or hypothetical cases.
7. Test the service behavior, API contract, failure path, and relevant isolation boundary.
8. Report any existing code that conflicts with this Skill instead of silently extending the conflict.

## Load the relevant standards

- Read [architecture.md](references/architecture.md) for modules, folders, SOLID, inheritance, and
  abstraction decisions.
- Read [backend-java.md](references/backend-java.md) for services, Lombok, mapping, JPA repositories,
  and Java implementation rules.
- Read [api-and-errors.md](references/api-and-errors.md) for DTOs, `BaseResponse`, `ResponseEntity`,
  exceptions, validation, and SSE.
- Read [security-and-sessions.md](references/security-and-sessions.md) for authentication transport,
  cookies, CSRF, browser storage, and sensitive client state.
- Read [frontend.md](references/frontend.md) for every frontend task.

## Guardrails

- Do not introduce a pattern merely because it is common.
- Do not create `ServiceImpl`, a matching interface, a mapper per DTO, or generic base CRUD layers
  without a demonstrated need.
- Do not place DTOs inside entities, services, controllers, or other classes.
- Do not expose entities through the API.
- Do not place business rules, persistence, mapping, or exception handling in controllers.
- Do not leak raw infrastructure exceptions, stack traces, secrets, or sensitive model fields.
- Do not store or send a web login bearer token through browser-managed JavaScript storage or an
  application-created `Authorization` header.
- Do not fight framework-required conventions such as Spring Data repository interfaces.
