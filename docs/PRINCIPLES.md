# Principles

1. **Learn before abstracting.** The first implementation should reveal the concept. Abstractions
   emerge when real use cases justify them.
2. **Evolve in small steps.** Each phase must work, have tests, and produce learning before the next.
3. **Local by default.** Data and models remain on the machine unless the user explicitly chooses
   otherwise.
4. **Permission before effect.** Actions that change files, processes, or services require a
   permission decision proportional to their risk.
5. **The model proposes; the system controls.** The LLM does not decide by itself what it may access
   or execute.
6. **Verifiable results.** Tools return structured evidence and explicit errors.
7. **Clear boundaries.** Conversation, model, RAG, tools, permissions, and execution have distinct
   responsibilities.
8. **Security from the beginning.** Isolation, validation, and auditing are not postponed.
9. **Living documentation.** Every important architectural decision records its context and
   consequences.
10. **Earned complexity.** Redis, queues, and multiple modules are introduced only when the problem
    requires them.

