# Architecture standards

## Modules and folders

- Apply SOLID pragmatically.
- Organize code by business module, then by technical responsibility inside that module.
- Split a large module into thematic submodules that group related code.
- Continue the same thematic hierarchy inside folders such as `dto` and `exception`.
- Keep module boundaries and dependencies explicit.

Example:

```text
auth/
  user/
    controller/
    service/
    repository/
    model/
    dto/
  token/
  access/
  session/
```

Do not create one global `controller`, `service`, `repository`, `dto`, or `model` directory that
mixes unrelated business capabilities.

## Abstraction rule

- Implement a one-off or trivial operation directly.
- Observe early repetition before extracting it.
- Extract stable, materially repeated behavior when the result stays easier to understand.
- Use a three-occurrence pattern only as a review signal, not an automatic threshold.
- Allow reusable concepts such as `GenericMapper`, `BaseResponse`, `BusinessException`, audit fields,
  or correlation filters when they remove meaningful repetition.
- Prefer composition, but allow inheritance for a true semantic relationship or genuinely shared
  behavior.
- Avoid `BaseService`, `AbstractCrudService`, `GenericCrudController`, and similar hierarchies that
  hide module-specific behavior.

## Framework rule

Follow the official shape of a selected framework or library when required or clearly recommended.
Do not wrap it only to rename or forward the same calls. Examples include Spring Data interfaces,
JPA constructors, Spring Security filter chains, React hooks, and TanStack Query primitives.

Priority:

1. security and correctness;
2. official library contract;
3. Nexo standards;
4. local preference.
