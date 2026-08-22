package com.nexoia.conversation.inference.context;

/**
 * The typed, per-request statement of what the model received: who the authenticated user is, the
 * conversation mode, and the resolved {@link CapabilityManifest}. Rendered deterministically into a
 * system message so the model's claims can be held to what actually happened.
 */
public record ModelContextEnvelope(
        String username,
        String conversationMode,
        CapabilityManifest manifest) {}
