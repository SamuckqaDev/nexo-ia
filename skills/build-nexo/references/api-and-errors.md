# API and error standards

## DTO organization

- Define every DTO as a separate top-level file, preferably a Java record.
- Never nest a DTO inside a controller, service, entity, or another model.
- Separate DTOs by module and theme such as `user`, `token`, and `access`.
- Add `request`, `response`, `event`, or another subfolder when the number of DTOs makes that grouping
  useful.
- Keep entities, domain models, and API DTOs distinct.

Example:

```text
auth/dto/
  user/CreateUserRequest.java
  user/UserResponse.java
  token/RefreshTokenRequest.java
  token/TokenResponse.java
  access/GrantAccessRequest.java
```

## Normal response contract

Return `ResponseEntity<BaseResponse<T>>` for ordinary HTTP endpoints. `BaseResponse` contains:

```java
public record BaseResponse<T>(
        int code,
        String message,
        List<T> data
) {}
```

- Keep the HTTP status and body `code` consistent.
- Return one object inside a one-element `data` array.
- Return multiple objects in the same array.
- Return an empty array for a successful operation without result data.
- Return `data: null` for an error.
- Use a body-bearing success status such as `200` when returning an empty `BaseResponse`; a true HTTP
  `204` has no body.
- Keep `BaseResponse` at the HTTP boundary; do not pass it through services or repositories.

## Controllers

- Keep route declarations, OpenAPI documentation, declarative authorization, structural request
  validation, service delegation, `ResponseEntity`, and response wrapping only.
- Do not place business validation, repository access, mapping, entity construction, calculations,
  or repetitive `try/catch` blocks in a controller.

## Exceptions

- Represent every known application failure with a personalized exception organized by module and
  theme.
- Use meaningful base exception types only when they support repeated handling.
- Group semantically equivalent base exception types explicitly in `@ExceptionHandler({...})`
  arrays. Keep a general `ApplicationException` fallback so a new personalized error is still
  controlled before it receives a dedicated category.
- Do not throw generic `Exception`, `RuntimeException`, or `IllegalArgumentException` as a business
  outcome.
- Handle application exceptions centrally with `@RestControllerAdvice`.
- Return controlled public messages and never raw stack traces, SQL, paths, secrets, or arbitrary
  third-party exception messages.
- Log unexpected failures with a correlation identifier and return a safe personalized internal
  error response.
- Do not swallow an error merely to keep execution moving; preserve transactional correctness and
  terminate an invalid operation safely.

## SSE and special responses

SSE is a formal exception to the normal `BaseResponse` envelope because the response remains open.
Use `ResponseEntity<SseEmitter>` or the selected Spring streaming contract and define every event as
a separate typed DTO.

- Before headers are committed, route a failure through the global exception handler.
- After streaming begins, emit a controlled typed error event, record the failure, and close the
  stream safely; the HTTP status can no longer be changed.
- Define typed started, content/token, usage, completed, cancelled, and error events as needed.
- Allow other explicit response exceptions for binary files, uploads/downloads, redirects, health
  protocols, or externally dictated callback contracts.
