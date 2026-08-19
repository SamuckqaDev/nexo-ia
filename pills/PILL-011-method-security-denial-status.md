# PILL-011 — A method-security denial must be handled, or it becomes a 500

- **Status:** accepted
- **Discovered:** 2026-08-19
- **Last reviewed:** 2026-08-19
- **Area:** security | Spring

## Question

Why did an Owner-only endpoint answer `500` instead of `403` when a Member called it, even though the
security filter chain already had an access-denied handler?

## Finding

`@PreAuthorize` throws `AccessDeniedException` at controller invocation, after the filter chain; the
filter-chain `AccessDeniedHandler` never sees it, so without an `@ExceptionHandler` the advice's
generic `Exception` branch turns it into a `500`.

## Evidence

- [Spring Security method security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security authorization architecture](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html)
- `AuditControllerTest.deniesAMember` reproduced the `500` before the fix.

## Explanation

Spring Security enforces authorization at two different points. The filter chain enforces
`authorizeHttpRequests(...)` rules and routes a denial through the configured
`AccessDeniedHandler` and `AuthenticationEntryPoint`. Method security — `@PreAuthorize`,
`@EnableMethodSecurity` — enforces at the point of the method call, which happens *inside* the
DispatcherServlet, well past the filter chain. A denial there is a thrown `AccessDeniedException`
that propagates like any other exception from the controller.

A `@RestControllerAdvice` with a catch-all `@ExceptionHandler(Exception.class)` will therefore
swallow it and return the generic internal-error response. The fix is an explicit
`@ExceptionHandler(AccessDeniedException.class)` that returns `403` with the standard envelope. The
message stays generic so it does not confirm the resource exists.

The subtle part is that the two handlers do not overlap: configuring the filter-chain
`AccessDeniedHandler` does nothing for method-security denials, and it is easy to assume one handler
covers both. Any project that mixes `authorizeHttpRequests` with `@PreAuthorize` needs the advice
branch as well.

## Impact on Nexo IA

- `GlobalExceptionHandler` handles `AccessDeniedException` and returns `403` with `BaseResponse`.
- Every `@PreAuthorize("hasRole('OWNER')")` endpoint — audit inspection and Member administration —
  now answers `403` to an unauthorized role instead of `500`.
- A cross-role test that asserts the `403` guards against a regression.

## Limits and review triggers

Review this if Nexo IA moves authorization entirely into the filter chain, or introduces a second
advice that also handles `AccessDeniedException`, since ordering between advices would then matter.
