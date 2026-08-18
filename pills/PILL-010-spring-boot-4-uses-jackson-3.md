# PILL-010 — Spring Boot 4 auto-configures Jackson 3, not Jackson 2

- **Status:** accepted
- **Discovered:** 2026-08-18
- **Last reviewed:** 2026-08-18
- **Area:** Spring | Java | operations

## Question

Which JSON mapper bean can a Spring Boot 4 application inject, and why did injecting `ObjectMapper`
fail at startup while every unit test passed?

## Finding

Spring Boot 4 auto-configures Jackson 3, whose mapper is `tools.jackson.databind.ObjectMapper`; the
Jackson 2 type of the same simple name, `com.fasterxml.jackson.databind.ObjectMapper`, has no bean
even when it is present on the classpath.

## Evidence

Dependency tree of this project:

```text
spring-boot-starter-jackson:4.1.0
  \- tools.jackson.core:jackson-databind:3.1.4
     +- com.fasterxml.jackson.core:jackson-annotations:2.21
     \- tools.jackson.core:jackson-core:3.1.4
```

Startup failure before the fix:

```text
Error creating bean with name 'ollamaChatCompletionClient': Unsatisfied dependency expressed
through constructor parameter 1: No qualifying bean of type
'com.fasterxml.jackson.databind.ObjectMapper' available
```

- [Jackson 3 release notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [Spring Boot 4 release notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)

## Explanation

Jackson 3 changed its base package from `com.fasterxml.jackson` to `tools.jackson`. Spring Boot 4
adopts it, so `JsonMapper` and `ObjectMapper` beans come from the new package. Annotations did not
move: `@JsonProperty` and `@JsonIgnoreProperties` still live in `com.fasterxml.jackson.annotation`,
which is why annotated DTOs compile unchanged and give no hint that the mapper type is different.

Jackson 2 can still appear on the classpath as a transitive dependency — here it arrives through
Spring AI's JSON-schema module. That makes the mistake easy: the import resolves, the code compiles,
and the failure only appears when the context starts.

The lesson generalises beyond Jackson. A unit test that constructs its collaborators directly proves
the logic but says nothing about whether the container can supply them. This project has no test that
starts the full application context, so the whole test suite stayed green while the application could
not boot. Running the built image is what caught it.

Jackson 3 also makes its exceptions unchecked: `JacksonException` extends `RuntimeException`, so
reading no longer forces an `IOException` catch. Stream reading still needs one for the underlying
I/O.

## Impact on Nexo IA

- `OllamaChatCompletionClient` injects `tools.jackson.databind.ObjectMapper`.
- Tests build a mapper with `JsonMapper.builder().build()` from the same package.
- DTO annotations remain on `com.fasterxml.jackson.annotation`.
- A context-loading test, or running the built image, is required before an increment that adds a new
  injected collaborator is considered verified.

## Limits and review triggers

Review this when Spring AI or another dependency stops pulling Jackson 2 in, when Nexo IA adds a
custom mapper configuration, or when a library requires a Jackson 2 mapper explicitly and the two
generations must coexist deliberately.
