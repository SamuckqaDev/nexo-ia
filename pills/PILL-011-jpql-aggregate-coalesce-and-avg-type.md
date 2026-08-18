# PILL-011 — JPQL aggregates need coalesce, and avg returns a Double

- **Status:** accepted
- **Discovered:** 2026-08-18
- **Last reviewed:** 2026-08-18
- **Area:** Java | Spring | operations

## Question

Why did a constructor-expression usage query fail to build, and then fail again on an empty
reporting window?

## Finding

`avg` returns `Double`, so a constructor-expression parameter typed as `Long` has no matching
constructor; and `sum` over zero rows returns `null`, so a primitive record field needs
`coalesce(sum(...), 0)`.

## Evidence

- Hibernate build error: `Missing constructor for type 'UsageTotals'`, raised at context startup
  because Spring Data validates `@Query` methods eagerly.
- Runtime error on an empty window: `NullPointerException ... Number.longValue()` while instantiating
  the record.
- [Jakarta Persistence — aggregate functions](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#aggregate-functions)
- `UsageRepositoryTest.reportsAnEmptyWindowAsZeroInsteadOfNull`
- [PILL-010](PILL-010-spring-boot-4-uses-jackson-3.md) — the same eager query validation caught this
  at startup rather than at first call.

## Explanation

A JPQL constructor expression, `SELECT new com.nexoia.usage.dto.UsageTotals(...)`, matches its
arguments to a constructor by type. The SQL aggregate `avg` is defined to return a double regardless
of the summed column's type, so a field declared `Long` silently fails the match and Hibernate
reports a missing constructor. The fix is to type averaged fields as `Double` — which also carries a
useful truth, since the average of no rows is `null`, not zero.

The second failure is subtler because it depends on the data. `count` over zero rows returns `0`,
but `sum` over zero rows returns `null`. A record with a primitive `long` field cannot receive that
`null`, so the query works for every non-empty window and throws only when a member has no usage yet
— exactly the empty state a usage screen must handle. Wrapping every `sum`, including the ones inside
`CASE` expressions, in `coalesce(sum(...), 0L)` makes the empty window aggregate to zeros. An average
stays nullable on purpose: "no data" is not "zero latency".

Both failures are cheap to catch precisely because Spring Data validates the query when the
repository bean is built. That is another reason a context-loading integration test is worth its
cost — see PILL-010.

## Impact on Nexo IA

- Averaged fields in usage DTOs are `Double` and nullable; the interface renders `null` as an em
  dash, never as `0`.
- Every `sum` in the usage queries, including those inside `CASE`, is wrapped in `coalesce(..., 0L)`.
- The empty-window case is an explicit repository test, not an afterthought.

## Limits and review triggers

Review this if Nexo IA moves aggregation into native SQL or a projection interface, where the null
and type rules differ, or if a reporting field legitimately needs to distinguish "zero" from "no
data" and therefore must stay nullable rather than being coalesced.
