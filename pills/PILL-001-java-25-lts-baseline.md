# PILL-001 — Java 25 LTS is the baseline for a new 2026 project

- **Status:** accepted
- **Discovered:** 2026-08-12
- **Last reviewed:** 2026-08-12
- **Area:** Java

## Question

Should Nexo IA remain on Java 21 or begin with Java 25?

## Finding

Nexo IA should use Java 25 because it is the newest LTS release available at the beginning of this
new project and the selected Spring Boot line officially supports it.

## Evidence

- [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Baeldung Java and Spring examples repository](https://github.com/eugenp/tutorials)

Oracle identifies Java 8, 11, 17, 21, and 25 as LTS releases. Its roadmap states that Java 25 was
released as LTS in September 2025. The current Spring Boot 4.1 system requirements state support for
Java versions from 17 through 26.

Recent Baeldung Spring AI 2.0 examples still compile with Java 21. That is useful compatibility
evidence: Spring AI does not require Java 25. It is not a reason for a new project to remain on the
older LTS when its selected framework supports the newer one.

## Explanation

An LTS baseline is more appropriate than a short-lived feature release for a project expected to be
studied and evolved over years. Starting on Java 25 avoids planning an early 21-to-25 migration while
still keeping a supported runtime boundary.

The project may use stable Java 25 language and runtime capabilities, but it must not adopt a new
feature merely because it exists. Each feature still needs a readability, testability, or operational
benefit.

## Impact on Nexo IA

- The compiler release is Java 25.
- Maven Enforcer rejects unsupported Java versions.
- CI and local setup use a documented Java 25 distribution.
- Production code does not use preview features.
- Compatibility with Java 21 is not a project requirement.

## Limits and review triggers

Review this pill when Nexo IA upgrades to a new Spring Boot major line or when the next Java LTS
becomes generally available and ecosystem support is established.

