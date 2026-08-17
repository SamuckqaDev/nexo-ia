# Frontend standards

## Already inherited from project-wide rules

- Organize the frontend by business module and split large modules by theme.
- Follow React, TypeScript, Vite, TanStack, and other selected libraries in their intended style.
- Abstract stable, materially repeated behavior; keep trivial and one-off behavior direct.
- Keep API contracts separate from view components.
- Treat SSE as a typed event flow rather than a normal one-shot `BaseResponse`.

## Modules and components

- Organize frontend code by business module and theme, following the same modular reasoning as the
  backend.
- Split large components into focused subcomponents owned by the closest parent feature.
- Keep a component or subcomponent in its own named folder.
- Use `index.tsx` for the component implementation and `styles.ts` for its styled definitions.
- Keep type definitions out of component folders and component implementation files.
- Store cross-application contracts in `shared/types` and domain-owned contracts in the closest
  module-level `types` folder. Split those files by theme instead of creating one global type dump.
- Derive types from Zod schemas in the owning module's `types` folder when inference is available;
  do not duplicate the contract manually.
- Explicitly type component props, function and hook returns, state shapes, callbacks, API inputs,
  and reusable contracts. Use inference only for obvious local literals where an annotation would
  repeat the same information without improving safety or readability.
- Do not use `any`. Use `unknown` only at genuinely untrusted boundaries such as external payloads
  and caught third-party errors, then narrow or validate it before use.
- Keep a subcomponent inside its parent's `components` folder when it is not reusable outside that
  parent or feature.
- Move a component to module-level or global shared code only after real reuse is established.

Example:

```text
modules/conversation/chat/
  components/
    MessageList/
      index.tsx
      styles.ts
      components/
        MessageItem/
          index.tsx
          styles.ts
    ChatComposer/
      index.tsx
      styles.ts
  pages/
    ConversationPage/
      index.tsx
      styles.ts
```

## Styling

- Use `styled-components` as the application styling system instead of CSS Modules.
- Keep every component's styled definitions in its adjacent `styles.ts` file.
- Define application-wide theme tokens through `ThemeProvider`; do not repeat raw brand values
  throughout component files.
- Keep global resets and genuinely global rules in one global-style definition.
- Use transient props such as `$active` for style-only values so they are not forwarded to the DOM.
- Avoid inline styles except when a truly dynamic browser value cannot be represented clearly by the
  styling system.
- Do not place component behavior or API calls in `styles.ts`.
- Import each styled component by its exported name. Do not use namespace aliases such as
  `import * as S` or JSX like `<S.Panel>`; prefer named imports such as `Panel` and `Copy` so the
  rendered structure stays visually clean.

## Responsibility and data flow

- Keep components focused on rendering, user interaction, accessibility, and small local UI state.
- Keep pages focused on composing module components and connecting route-level inputs.
- Put reusable behavior, server-state orchestration, mutations, streaming lifecycle, and API
  integration in named hooks.
- Use TanStack Query through module hooks for ordinary server state; do not fetch server data directly
  inside visual components.
- Do not move a trivial toggle, input focus, or other strictly local state into a custom hook merely
  to follow a pattern.
- Keep business authorization and authoritative validation in the backend. Frontend hooks coordinate
  the experience but do not become a security boundary.
- Return explicit state from hooks, such as data, loading, error, action, and action progress; do not
  make components infer lifecycle state from unrelated values.

Preferred flow:

```text
Page -> component -> module hook -> TanStack Query/API client -> backend
```

- Configure the shared API client for cookie-based sessions and the approved CSRF mechanism.
- Never persist or construct a web authentication bearer token in frontend code.
- Keep theme and other non-sensitive visual preferences separate from authenticated session state.
- Prefer Promise chains with `.then()`, `.catch()`, and `.finally()` in React frontend code. Avoid
  `try/catch` and `async/await` unless a selected library contract or a genuinely clearer sequential
  flow requires them.

## HTTP client

- Use Axios as the standard client for ordinary HTTP commands and queries.
- Create one shared configured Axios instance under `shared/api`; do not instantiate Axios in every
  module or component.
- Configure `withCredentials`, base URL, timeout, JSON defaults, and the approved CSRF cookie/header
  names centrally.
- Keep authentication session cookies browser-managed; never add a bearer token interceptor.
- Use interceptors only for repeated transport concerns such as correlation, CSRF integration, safe
  error normalization, and observability. Do not place business rules, navigation policy, UI
  notifications, or module-specific behavior in a global interceptor.
- Keep endpoint functions in the owning module's `api` folder and call only the shared client there.
- Type ordinary responses as `BaseResponse<T>` and preserve the backend's one-item array, collection,
  empty-array, and error-null contract.
- Normalize Axios/network failures to a project-owned frontend error type without discarding the
  backend's safe code and message.
- Support cancellation through `AbortSignal` for queries or operations that can be superseded.
- Do not use Axios for browser SSE. Use a dedicated typed streaming client that supports cookie
  sessions, cancellation, reconnection policy, and the Nexo event contracts.

Example:

```text
shared/api/
  client.ts
  BaseResponse.ts
  ApiError.ts
modules/auth/user/api/
  userApi.ts
```

## Forms and validation

- Use React Hook Form for application forms.
- Use Zod for frontend form schemas and runtime validation of untrusted boundaries.
- Integrate both through the supported Hook Form Zod resolver rather than copying validation rules
  into event handlers.
- Keep schemas in the owning module's `schemas` folder and name them after the operation.
- Infer the form value type from the Zod schema when the shapes are identical; do not manually
  duplicate the same type.
- Keep API response types distinct when their contract differs from form input.
- Put reusable form orchestration and mutation behavior in a named module hook; keep field rendering,
  labels, help, and accessibility in components.
- Map controlled backend validation errors to the appropriate fields when the error contract provides
  safe field details, and retain a form-level error for failures without a field.
- Disable or guard duplicate submission while a mutation is pending and provide explicit success and
  failure feedback.
- Preserve entered values after a recoverable server failure unless clearing them is a deliberate
  security rule, such as for a password.
- Treat frontend validation as user feedback, never as the authoritative security or business-rule
  boundary; validate again in the backend.
- Use Zod at selected API, imported configuration, persisted preference, and SSE event boundaries
  where runtime data cannot be trusted. Do not parse the same already-validated value repeatedly at
  every component layer.

Example:

```text
modules/auth/user/
  schemas/createUserSchema.ts
  hooks/useCreateUserForm.ts
  components/UserForm/index.tsx
  components/UserForm/styles.ts
```

## State ownership

- Use TanStack Query as the owner of server state, including fetched resources, mutations, caching,
  invalidation, retries, and request lifecycle.
- Use Zustand for genuinely shared client state that must survive across unrelated component trees,
  such as global workspace UI state or a multi-surface draft that is not yet server-owned.
- Use component state for local and short-lived visual state.
- Do not copy TanStack Query resources into Zustand.
- Do not create a global store for one component or one page.
- Organize Zustand stores by owning module and keep only truly cross-cutting stores under `shared` or
  `app`.
- Expose focused selectors so a component subscribes only to the state it needs.
- Keep business authorization and durable product state on the backend; a Zustand value is never a
  permission or security boundary.
- Persist only an explicitly reviewed subset of non-sensitive client state. Never persist session
  identifiers, secrets, provider credentials, permissions, or private model context in a Zustand
  storage adapter.

Selection rule:

```text
Backend resource -> TanStack Query
Cross-screen client-only state -> Zustand
Component-only state -> useState/useReducer
Form state -> React Hook Form
```

## Unspecified choices

For details not defined here—such as exact cache durations, responsive breakpoints, loading visuals,
or test naming—follow the selected library's official contract and the existing local convention.
Choose the simplest accessible implementation that fits the feature, and do not promote a new
project-wide rule without repeated evidence or creator direction.
