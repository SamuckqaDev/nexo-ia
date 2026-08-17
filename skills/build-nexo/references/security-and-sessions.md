# Security and session transport

## Web authentication

- Authenticate the web application through a server-tracked, revocable token session.
- Send the short-lived access JWT only through a cookie configured with `HttpOnly`, an appropriate
  `SameSite` policy, `Secure` under HTTPS, an explicit path, and expiry.
- Send a high-entropy opaque refresh token through a narrower `HttpOnly` cookie. Store only its
  cryptographic hash, rotate it on every use, retain its replacement chain, and revoke the complete
  session when reuse is detected.
- Keep an access-token `jti` and session status server-side so logout, user disablement, compromise,
  and administrative revocation take effect before JWT expiry.
- Never persist raw access JWTs or raw refresh tokens in the database, audit events, or logs.
- Keep the authenticated user, organization, roles, permissions, and sensitive session state on the
  server, not inside a client-readable cookie.
- Let the browser attach the cookie automatically. Configure the API client to include credentials
  where cross-origin development or deployment requires it.
- Do not store a login token in `localStorage`, `sessionStorage`, IndexedDB, ordinary JavaScript
  memory intended for persistence, or another client-readable persistence mechanism.
- Do not build an `Authorization: Bearer` header for the normal Nexo web session.
- Record controlled session and access metadata such as initial IP, last IP, user agent, issuance,
  last-seen, expiry, rotation, revocation, and security-relevant event type. Trust forwarded client
  addresses only through an explicitly configured trusted proxy boundary.

## CSRF and protocol headers

- Enable Spring Security CSRF protection for state-changing browser requests.
- Permit a dedicated CSRF header as a security requirement; it is not an authentication bearer token.
- Keep `SameSite` as defense in depth rather than treating it as the only CSRF control.
- Permit ordinary protocol headers such as `Content-Type`, `Accept`, cache validators, correlation,
  idempotency, and standards-required streaming or file headers when justified.
- Never weaken CSRF merely to enforce a literal no-header rule.

## Data placement

- Send business input through the validated request body, path, or query contract as appropriate.
- Do not use cookies as a general business-data transport or authorization source.
- Treat every client-provided cookie, body value, path, query, and header as untrusted input.
- Store non-sensitive visual preferences, such as light or dark theme, locally when server sync is not
  required. Prefer the operating-system preference as the initial default.
- Never place secrets, permissions, provider credentials, private context, or authoritative policy in
  theme/preference storage.
