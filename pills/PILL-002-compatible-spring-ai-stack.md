# PILL-002 — Select Spring Boot and Spring AI as a compatibility pair

- **Status:** accepted
- **Discovered:** 2026-08-12
- **Last reviewed:** 2026-08-12
- **Area:** Spring AI

## Question

Can Nexo IA independently choose the newest Spring Boot and Spring AI versions?

## Finding

No. Spring Boot and Spring AI must be selected as a documented compatibility pair and their
dependency versions must come from their BOMs.

## Evidence

- [Spring AI getting started](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Baeldung Spring AI examples](https://github.com/eugenp/tutorials/tree/master/spring-ai-modules)

The current Spring AI documentation states that Spring AI 2.0.x supports Spring Boot 4.0.x and 4.1.x.
It also provides a Spring AI BOM for the recommended versions of its related artifacts. The current
Spring Boot documentation identifies 4.1.0 as its reference line.

Recent Baeldung modules provide practical confirmation by combining Spring AI 2.0.0 with Spring Boot
4.0.x for MCP and Agent Skills examples. Older modules in the same repository still contain Spring
AI milestones and Spring Boot 3.x, demonstrating why copying versions from an individual tutorial is
unsafe.

## Explanation

Spring projects are collections of coordinated artifacts. Manually assigning versions to every
starter increases the risk of binary incompatibility and transitive dependency conflicts. A BOM
provides the tested version set while the project declares only direct dependencies.

## Impact on Nexo IA

- Initial baseline: Spring Boot 4.1.0 and Spring AI 2.0.0.
- Use the Spring Boot parent POM and import the Spring AI BOM.
- Do not assign versions to BOM-managed Spring dependencies.
- Add an application-context smoke test before accepting dependency upgrades.
- Upgrade Boot and Spring AI in a dedicated compatibility change, not incidentally.

## Limits and review triggers

Confirm the compatibility table again immediately before generating the project because a newer
patch release may exist. Prefer the newest stable patch inside the accepted 4.1.x and 2.0.x lines.

