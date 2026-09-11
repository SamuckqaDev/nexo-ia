package com.nexoia.provider.model;

public enum ProviderType {
    OLLAMA,
    OPENAI,
    GOOGLE_GEMINI,
    ANTHROPIC,
    OPENAI_COMPATIBLE;

    public boolean requiresCredential() {
        return this == OPENAI || this == GOOGLE_GEMINI || this == ANTHROPIC;
    }

    public boolean usesOpenAiProtocol() {
        return this == OPENAI || this == GOOGLE_GEMINI || this == OPENAI_COMPATIBLE;
    }
}
