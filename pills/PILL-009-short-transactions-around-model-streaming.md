# PILL-009 — Short transactions around model streaming

- **Status:** accepted
- **Discovered:** 2026-08-18
- **Last reviewed:** 2026-08-18
- **Area:** architecture | Spring | Java | operations

## Question

How should a request that streams a model answer for minutes be wrapped in transactions?

## Finding

Two short transactions with the stream between them, in a separate bean: a transaction must never
stay open while tokens arrive, and a self-invoked `@Transactional` method silently runs without one.

## Evidence

- [Spring transaction propagation and proxying](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)
- [Spring: understanding the transactional proxy](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [HikariCP pool sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- `ModelRequestServiceTest`, `ModelRequestStore`
- Release `0.1` gate: *shutdown does not leave a model request falsely marked as completed*.

## Explanation

A local model can stream for minutes. A `@Transactional` method that wraps the whole request holds a
pooled database connection for that entire time, and it also holds the pessimistic conversation lock
taken to order messages — see [PILL-006](PILL-006-pessimistic-lock-for-ordered-appends.md). With a
default pool of ten connections, ten concurrent generations exhaust the pool and every unrelated
request in the application starts waiting. The failure looks like a database problem, but the cause
is the transaction boundary.

The fix is to split the work: one transaction appends the user message and reserves the assistant
message, the stream runs with no transaction and no lock, and a second short transaction records the
terminal state. Values, not entities, cross that boundary — the streaming stage has no persistence
context, so passing a managed entity would produce a detached instance and lazy-loading failures.

The second half of the finding is a Spring detail that is easy to get wrong. `@Transactional` is
applied by a proxy around the bean, so it only takes effect on calls that arrive from outside. A
private or protected `@Transactional` method invoked as `this.reserve(...)` from another method of
the same class bypasses the proxy entirely and runs with no transaction at all — silently, with no
warning at startup or at runtime. Putting the transactional methods in their own bean, injected as a
collaborator, makes every call go through the proxy.

That split has a useful side effect: orchestration and persistence become separately testable, so
the streaming lifecycle can be tested with a mocked store and no database.

## Impact on Nexo IA

- `ModelRequestStore` owns every `@Transactional` method of a model request.
- `ModelRequestService` orchestrates and holds no transaction while streaming.
- `ModelRequestReservation` carries plain values across the boundary, never entities.
- Streaming runs on `Executors.newVirtualThreadPerTaskExecutor()`, which is affordable precisely
  because no connection is pinned to the blocked thread.
- `@PreDestroy` fails in-flight requests so a restart cannot leave a request looking complete.

## Limits and review triggers

Review this when Nexo IA introduces multi-instance deployment, where the in-memory cancellation
registry stops being sufficient and shutdown handling must consider requests owned by another
instance. Also review it if a future capability needs the model output and a database change to
commit atomically, which would require an outbox rather than a longer transaction.
