# Backend Java standards

## Services

- Put business rules, feature behavior, orchestration, transactions, repository access, and mapping
  in the owning service.
- Keep controllers limited to the HTTP contract and documentation.
- Use concrete classes named for the business capability, such as `UserService`.
- Do not create a same-purpose interface and `ServiceImpl` by default.
- Introduce an interface for a real provider boundary, multiple implementations, replaceable adapter,
  module contract, or another concrete need.
- Return response DTOs, never JPA entities, to controllers.
- Throw personalized exceptions for every known application failure.

## Lombok and records

- Use Lombok to remove meaningful Java boilerplate.
- Prefer `@RequiredArgsConstructor`, `@Getter`, `@Slf4j`, and narrowly justified constructors or
  builders.
- Use Java records for DTOs; do not add Lombok to records without a specific need.
- Avoid blanket `@Data` on JPA entities because generated setters, `toString`, `equals`, and
  `hashCode` can expose sensitive fields or traverse lazy relationships.
- Use Hibernate `@CreationTimestamp` and `@UpdateTimestamp` for ordinary technical entity audit
  timestamps. Do not assign those timestamps manually unless a business rule requires a user- or
  event-supplied time.

## Generic mapping

- Provide one project-owned `GenericMapper` for objects and collections.
- Do not create a mapper class for each DTO or entity.
- Place no business rule or repository call inside mapping.
- Convert mapping failures to `MappingException`.
- Configure exclusions and special conversions centrally, especially secrets, password hashes,
  tokens, internal policy fields, and lazy relationships.
- Fail explicitly when a safe generic conversion is impossible; never return a silently partial
  object.
- Select the underlying library after comparing record support, strictness, nested conversion,
  security exclusions, maintenance, and runtime behavior. Do not expose that library throughout the
  application.

## Persistence

- Use Spring Data JPA repository interfaces directly for ordinary persistence.
- Prefer inherited CRUD, derived queries, and clear `@Query` methods before custom implementations.
- Add a custom repository only for a query that cannot remain clear or efficient through standard
  Spring Data contracts.
- Keep business rules, API responses, HTTP status decisions, and API DTOs out of repositories.
- Translate meaningful persistence failures into personalized application exceptions at the
  appropriate boundary.
