# PILL-012 — An unbindable request parameter is a 400, not a 500

- **Status:** accepted
- **Discovered:** 2026-08-18
- **Last reviewed:** 2026-08-18
- **Area:** Spring | security | frontend

## Question

What status should an endpoint return when a query parameter cannot be bound to its type, such as an
unknown enum value or a malformed UUID?

## Finding

`400 Bad Request`: the request is malformed, so leaving it to the catch-all handler that returns
`500` both misreports the cause and risks echoing the bad value.

## Evidence

- `UsageControllerTest.rejectsAnUnknownPeriod` — `?period=LAST_YEAR` returned `500` before the fix.
- Spring raises `MethodArgumentTypeMismatchException` for a parameter that cannot be converted.
- [RFC 9110 §15.5.1 — 400 Bad Request](https://www.rfc-editor.org/rfc/rfc9110#name-400-bad-request)
- [Spring MVC exception handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)

## Explanation

Binding runs before the controller method. When `?period=LAST_YEAR` cannot be converted to the enum,
Spring throws `MethodArgumentTypeMismatchException`, and without a dedicated handler it falls through
to the `Exception` catch-all that returns `500`. That is wrong twice: a `500` tells the client the
server failed when the client sent a bad request, and monitoring built on `5xx` rates fires on what
is really user input.

There is a security edge too. The default message for this exception can include the offending value
and the target type. Reflecting attacker-controlled input into a response body is a small XSS and
information-disclosure surface, so the handler names only the parameter and never echoes the value.

A caveat: this exception covers path and query binding. A malformed JSON body raises
`HttpMessageNotReadableException`, and a bean-validation failure raises
`MethodArgumentNotValidException`, which this project already handles. The request-body cases are
separate handlers.

## Impact on Nexo IA

- `GlobalExceptionHandler` maps `MethodArgumentTypeMismatchException` to `400` with a message that
  names the parameter only.
- The rule applies to every endpoint, so any future enum, UUID, or numeric query parameter reports a
  client error correctly.
- Frontend Zod enums mirror the backend enums, so a valid interface never sends an unbindable value;
  the backend guard is the authoritative boundary regardless.

## Limits and review triggers

Review this if Nexo IA adds a request-body binding path that needs the same treatment, or if a
parameter should accept an unknown value leniently rather than rejecting it — a deliberate choice
that would be recorded where it is made.
