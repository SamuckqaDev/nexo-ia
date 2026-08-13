# PILL-004 — What Java 25 adds beyond Java 21

- **Status:** accepted
- **Discovered:** 2026-08-12
- **Last reviewed:** 2026-08-12
- **Area:** Java

## Question

What are the practical differences between Java 21 and Java 25 for Nexo IA?

## Finding

Java 25 keeps Java 21's major foundations—records, pattern matching, and virtual threads—but adds four
releases of finalized APIs and runtime improvements, especially Scoped Values, unpinned virtual-thread
synchronization, Stream Gatherers, Class-File API, AOT improvements, and newer GC and JFR capabilities.

## Evidence

- [OpenJDK 22 features](https://openjdk.org/projects/jdk/22/)
- [OpenJDK 23 features](https://openjdk.org/projects/jdk/23/)
- [OpenJDK 24 features](https://openjdk.org/projects/jdk/24/)
- [OpenJDK 25 features](https://openjdk.org/projects/jdk/25/)
- [JEPs delivered since Java 21](https://openjdk.org/projects/jdk/25/jeps-since-jdk-21)

## Main finalized changes

### Language

- **Unnamed variables and patterns:** use `_` when a value is intentionally ignored.
- **Flexible constructor bodies:** validate or prepare values before explicitly invoking another
  constructor, subject to initialization safety rules.
- **Module import declarations:** compact access to exported packages from a module. This is more
  useful for learning and compact programs than for Nexo IA production code.
- **Compact source files and instance main methods:** reduce ceremony for small programs and teaching
  examples, not for the application architecture.
- **Markdown documentation comments:** allow Markdown in Java documentation comments.

### Concurrency

- **Scoped Values:** a final API in Java 25 for immutable context shared through a bounded call tree.
  They can carry request or run context without relying on mutable `ThreadLocal` state.
- **Virtual threads without `synchronized` pinning:** Java 24 changed virtual-thread behavior so
  blocking inside ordinary synchronized code no longer pins the carrier thread in the previous way.
- **Structured Concurrency remains preview in Java 25.** Nexo IA must not use it while preview features
  are disabled.

### Data processing and integration

- **Stream Gatherers:** final in Java 24, extending streams with custom intermediate operations such
  as windows, folds, and stateful transformations.
- **Foreign Function and Memory API:** final in Java 22, providing a supported alternative to JNI for
  native libraries and off-heap memory when a real integration requires it.
- **Class-File API:** final in Java 24 for reading, generating, and transforming class files without a
  third-party bytecode API in appropriate tooling scenarios.

### Runtime and operations

- **Generational ZGC:** became the default ZGC mode in Java 23; the non-generational mode was removed
  in Java 24.
- **Generational Shenandoah:** finalized in Java 25.
- **Compact Object Headers:** finalized in Java 25 and can reduce object footprint when enabled and
  validated for the runtime.
- **Ahead-of-time improvements:** class loading/linking, command-line ergonomics, and method profiling
  provide new startup and warm-up optimization paths.
- **JFR improvements:** Java 25 adds CPU-time profiling, cooperative sampling, and method timing and
  tracing capabilities with different maturity levels.

### Security and compatibility

- **Security Manager is permanently disabled from Java 24.** It cannot be Nexo IA's tool sandbox.
- Use of JNI and unsafe memory access is being restricted or warned more strongly. Native integrations
  must use supported APIs and explicit operational boundaries.
- Java 25 removes the 32-bit x86 port.

## What Java 25 does not make automatically safe or fast

- Virtual threads do not make CPU-bound model inference faster.
- Scoped Values do not persist agent state or replace database ownership fields.
- Compact object headers and alternative collectors require measurement before enabling.
- AOT improvements do not eliminate application warm-up or dependency compatibility work.
- A newer JDK does not sandbox shell commands, MCP tools, filesystem access, or native processes.

## Impact on Nexo IA

- Use Java 25 without preview features.
- Consider Scoped Values for immutable trace and run context only after comparing them with explicit
  method parameters and framework context propagation.
- Use virtual threads for suitable blocking I/O workers, with load and cancellation tests.
- Keep Structured Concurrency out of production until it becomes final or the project explicitly
  revises its no-preview rule.
- Do not design the Permission Engine around Security Manager. Tool isolation requires canonicalized
  scopes, process boundaries, OS permissions, timeouts, environment filtering, and audit.
- Treat Stream Gatherers, FFM, Class-File API, AOT, and GC options as targeted tools, not defaults.

## Limits and review triggers

Review when the project benchmarks its first concurrent agent worker, adopts native libraries, or
upgrades to another Java LTS release.
