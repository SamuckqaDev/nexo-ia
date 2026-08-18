# PILL-007 — Resource isolation answers 404, never 401

- **Status:** accepted
- **Discovered:** 2026-08-18
- **Last reviewed:** 2026-08-18
- **Area:** security | frontend | Spring

## Question

Which HTTP status should Nexo IA return when an authenticated user requests a resource that belongs
to another user?

## Finding

`404 Not Found`: `401 Unauthorized` means the request lacks valid authentication, so returning it for
an authorization outcome makes a correctly authenticated user appear logged out.

## Evidence

- [RFC 9110 §15.5.2 — 401 Unauthorized](https://www.rfc-editor.org/rfc/rfc9110#name-401-unauthorized)
- [RFC 9110 §15.5.5 — 404 Not Found](https://www.rfc-editor.org/rfc/rfc9110#name-404-not-found)
- `ConversationServiceTest.hidesAnotherUsersConversationFromMessageReads`
- Release `0.1` requirement: cross-user access must fail *without revealing whether the resource
  exists* — [MVP and release strategy](../docs/MVP_AND_RELEASE_STRATEGY.md).

## Explanation

`ConversationNotFoundException` originally extended `UnauthorizedApplicationException`, so requesting
another user's conversation answered `401`. The frontend treats `401` as a transport-level signal
that the session ended: `ProviderRegistry` and other surfaces call `openSessionExpired()` on any
`ApiError` with status `401`. A user who merely followed a stale conversation link was therefore told
their session had expired and pushed back to login, while their session was in fact valid.

The distinction is not stylistic. `401` is about *authentication* and per RFC 9110 must carry a
`WWW-Authenticate` challenge — the client is expected to retry with credentials. `403` is about
*authorization* but confirms the resource exists. `404` reveals nothing, which is exactly the
required behaviour for resources isolated by ownership.

Returning `404` is safe here because the ownership filter lives in the query itself
(`findByIdAndUserId...`). The service cannot distinguish "does not exist" from "belongs to someone
else", so it cannot leak the difference even accidentally.

## Impact on Nexo IA

- `ConversationNotFoundException` and `ProviderConfigurationNotFoundException` extend
  `ApplicationException` with `HttpStatus.NOT_FOUND`.
- `UnauthorizedApplicationException` is reserved for genuine authentication failures: missing,
  invalid, expired, or revoked credentials.
- Ownership is enforced in the repository query, never by filtering after the fetch.
- Frontend `401` handling may keep treating the status as an expired session, because the backend no
  longer overloads it.

## Limits and review triggers

Review this when Nexo IA introduces shared resources with explicit access-control entries. A resource
a user can legitimately know about but may not act on is a `403` case, and the choice between `403`
and `404` then becomes a per-resource decision recorded with its sharing model.
