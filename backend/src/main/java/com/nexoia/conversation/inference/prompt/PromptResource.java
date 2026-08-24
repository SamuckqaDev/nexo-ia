package com.nexoia.conversation.inference.prompt;

/**
 * A versioned prompt fragment loaded from {@code classpath:/prompts}. Keeping these as Markdown
 * resources rather than Java text lets the assistant's identity, rules, and framing be edited and
 * reviewed without recompiling.
 */
public enum PromptResource {
    IDENTITY("prompts/nexo-identity.md"),
    RULES("prompts/nexo-rules.md"),
    AGENT_RULES("prompts/nexo-agent-rules.md"),
    PERSONAL_MEMORY_CONTEXT("prompts/personal-memory-context.md"),
    KNOWLEDGE_CONTEXT("prompts/knowledge-context.md"),
    CAPABILITY_ENVELOPE("prompts/capability-envelope.md");

    private final String path;

    PromptResource(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
