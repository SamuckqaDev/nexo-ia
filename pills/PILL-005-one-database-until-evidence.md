# PILL-005 — One application database until evidence requires another

- **Status:** accepted
- **Discovered:** 2026-08-16
- **Last reviewed:** 2026-08-16
- **Area:** architecture | RAG | operations

## Question

Should Nexo IA add MongoDB or Redis to PostgreSQL in its initial stack?

## Finding

No: PostgreSQL covers the transactional product domain, flexible JSON data, full-text search, and the
accepted future vector path, while another datastore creates costs without an accepted MVP need.

## Evidence

- [PostgreSQL JSON types](https://www.postgresql.org/docs/current/datatype-json.html)
- [PostgreSQL full-text search](https://www.postgresql.org/docs/current/textsearch.html)
- [pgvector project](https://github.com/pgvector/pgvector)
- [Spring AI PGvector](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)

## Explanation

Nexo IA's users, organizations, sessions, conversations, messages, usage, and audit records have
relational ownership and consistency rules. PostgreSQL models those rules directly. `jsonb` handles
selected provider metadata and evolving payloads without converting the complete product into an
unstructured document model. Later, `pgvector` keeps embedding search beside authorized metadata and
transactional ingestion state.

Redis can be excellent for shared low-latency cache, streams, and coordination, and MongoDB can be a
valid document database. Those strengths do not establish a requirement. A second datastore also
adds deployment, monitoring, isolation, backup, restore, security patching, and failure semantics.

## Impact on Nexo IA

- Release `0.1` uses PostgreSQL only.
- Release `0.2` may enable `pgvector` for Knowledge Vault retrieval.
- Flexible fields use `jsonb` selectively; core ownership and permissions remain normalized.
- Redis requires a Nexo-specific benchmark and cross-process operational need.
- MongoDB requires a future capability that PostgreSQL demonstrably cannot serve well.

## Limits and review triggers

Review this decision when measured workload shows PostgreSQL cannot meet a defined latency,
throughput, data-shape, stream, or availability target after normal schema and query optimization.
Preference or hypothetical scale alone is not a trigger.

