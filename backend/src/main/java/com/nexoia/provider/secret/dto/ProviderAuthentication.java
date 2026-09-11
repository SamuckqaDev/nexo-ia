package com.nexoia.provider.secret.dto;

/**
 * A request-scoped credential wrapper whose string representation is always redacted. It is never
 * serialized, persisted in a conversation, or included in model context.
 */
public final class ProviderAuthentication {

    private static final ProviderAuthentication NONE = new ProviderAuthentication("");

    private final String apiKey;

    private ProviderAuthentication(String apiKey) {
        this.apiKey = apiKey;
    }

    public static ProviderAuthentication none() {
        return NONE;
    }

    public static ProviderAuthentication apiKey(String apiKey) {
        return new ProviderAuthentication(apiKey);
    }

    public boolean configured() {
        return !apiKey.isBlank();
    }

    public String revealApiKey() {
        return apiKey;
    }

    @Override
    public String toString() {
        return configured() ? "ProviderAuthentication[REDACTED]" : "ProviderAuthentication[NONE]";
    }
}
