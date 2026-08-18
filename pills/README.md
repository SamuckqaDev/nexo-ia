# Nexo IA learning pills

Learning pills are short, focused records of knowledge discovered while designing, implementing,
testing, and operating Nexo IA.

They are not general tutorials or copies of documentation. Each pill must answer one concrete
question and explain how the answer affects the project.

## Format

Every pill contains:

- the question or problem;
- the finding in one sentence;
- evidence and sources;
- a practical explanation;
- the impact on Nexo IA;
- limitations or conditions that could invalidate the finding;
- the discovery date and review status.

Use [TEMPLATE.md](TEMPLATE.md) when adding a pill.

## Source priority

1. Specifications and official project documentation.
2. Official release notes and source repositories.
3. Reproducible experiments in Nexo IA.
4. High-quality practical references such as Baeldung.
5. Community discussion used only as supporting context.

A practical article can demonstrate an approach, but it must not override an official compatibility
contract or become the sole source for a security-sensitive decision.

## Index

| ID | Topic | Status |
|---|---|---|
| [PILL-001](PILL-001-java-25-lts-baseline.md) | Why Java 25 LTS is the Nexo IA baseline | Accepted |
| [PILL-002](PILL-002-compatible-spring-ai-stack.md) | Selecting compatible Spring Boot and Spring AI lines | Accepted |
| [PILL-003](PILL-003-baeldung-as-practical-reference.md) | How to use Baeldung without copying stale versions | Accepted |
| [PILL-004](PILL-004-java-21-to-25.md) | What Java 25 adds beyond Java 21 | Accepted |
| [PILL-005](PILL-005-one-database-until-evidence.md) | Why the baseline uses one application database | Accepted |
| [PILL-006](PILL-006-pessimistic-lock-for-ordered-appends.md) | Pessimistic locking for ordered message appends | Accepted |
| [PILL-007](PILL-007-not-found-instead-of-unauthorized.md) | Resource isolation answers 404, never 401 | Accepted |
| [PILL-008](PILL-008-ollama-ndjson-streaming-contract.md) | The Ollama chat streaming and token accounting contract | Accepted |
