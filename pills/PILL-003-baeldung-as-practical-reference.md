# PILL-003 — Baeldung is a practical reference, not a version authority

- **Status:** accepted
- **Discovered:** 2026-08-12
- **Last reviewed:** 2026-08-12
- **Area:** engineering practice

## Question

How should Nexo IA use Baeldung during development?

## Finding

Use Baeldung to discover and compare practical Java implementations, then verify contracts,
versions, security, and lifecycle behavior in the official documentation and project tests.

## Evidence

- [Baeldung tutorials source repository](https://github.com/eugenp/tutorials)
- [Spring AI reference](https://docs.spring.io/spring-ai/reference/)
- [Model Context Protocol SDKs](https://modelcontextprotocol.io/docs/sdk)

The Baeldung repository contains useful examples for Ollama chat, streaming, RAG, vector stores,
evaluation, MCP, Agent Skills, and Testcontainers. It also contains modules created at different
times with Spring AI milestone, 1.x, and 2.x releases and Spring Boot 3.x and 4.x releases.

## Explanation

Tutorial code optimizes for teaching one topic. Production architecture must additionally handle
ownership, failure recovery, observability, permissions, cancellation, migrations, and tests. An
article may also remain valuable after its dependency versions become old.

The correct workflow is:

1. Use Baeldung to understand an approachable example.
2. Open the linked source and identify the exact versions and assumptions.
3. Confirm the current contract in the official documentation.
4. reproduce the behavior in a focused Nexo IA experiment or test;
5. design the production boundary from Nexo IA's requirements.

## Impact on Nexo IA

- Every technical decision distinguishes primary and complementary sources.
- Copied tutorial code is not accepted without understanding, adaptation, and tests.
- Security and compatibility decisions require official sources.
- Useful discoveries become independent pills rather than undocumented conventions.

## Limits and review triggers

This source policy is permanent unless the project adopts a stricter evidence standard.

