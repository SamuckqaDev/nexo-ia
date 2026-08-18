# PILL-006 — Pessimistic locking for ordered message appends

- **Status:** accepted
- **Discovered:** 2026-08-18
- **Last reviewed:** 2026-08-18
- **Area:** architecture | Java | Spring | security

## Question

How should Nexo IA assign the sequence number of a conversation message so concurrent requests
cannot corrupt message order?

## Finding

Read the current highest sequence inside a pessimistic write lock on the conversation row; an
unlocked `COUNT` or an optimistic version both produce a wrong outcome for an ordered append.

## Evidence

- [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Jakarta Persistence `LockModeType`](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype)
- [PostgreSQL explicit locking](https://www.postgresql.org/docs/current/explicit-locking.html)
- `ConversationServiceTest.takesTheConversationWriteLockBeforeAppendingAMessage`
- Release `0.1` requirement: *prevent concurrent requests from corrupting message order or
  conversation state* — [MVP and release strategy](../docs/MVP_AND_RELEASE_STRATEGY.md).

## Explanation

The first implementation derived the sequence from `countByConversationId(id) + 1`. That read takes
no lock, so two requests on the same conversation can both observe the same count, both compute the
same sequence, and the second insert violates the unique `(conversation_id, sequence_number)`
constraint. The user sees an unhandled `500`.

Optimistic locking through `@Version` detects the conflict but resolves it the wrong way for this
operation: the losing request fails and the message the user typed is discarded. An append to an
ordered log has no meaningful merge, and there is nothing for the user to retry that they did not
already do.

A pessimistic write lock on the conversation row makes concurrent appends queue instead of collide.
`SELECT ... FOR UPDATE` on one conversation does not block a different conversation, so contention
stays proportional to real concurrency inside a single conversation, which is naturally low.

Two rules follow. The highest-sequence read is only stable while the lock is held, so the lock must
be acquired first and both operations must share one transaction. Readers must not take the lock,
otherwise listing a conversation would serialize against writes for no benefit.

## Impact on Nexo IA

- `ConversationRepository.findOwnedForUpdate` is the only entry point for conversation writes:
  message append, model selection, rename, and archive.
- `ConversationRepository.findByIdAndUserIdAndArchivedFalse` remains the read path and takes no lock.
- `ConversationMessageRepository.findHighestSequenceNumber` documents that callers must already hold
  the lock.
- The transaction that holds the lock must stay short. Model streaming runs outside it; see
  [PILL-008](PILL-008-short-transactions-around-model-streaming.md).

## Limits and review triggers

This holds while one application instance owns a conversation write and PostgreSQL row locks are the
coordination mechanism. Review it if Nexo IA runs conversation writes across instances with a
different coordination model, if message appends become batched, or if a conversation gains a
concurrent multi-writer feature such as shared editing.
