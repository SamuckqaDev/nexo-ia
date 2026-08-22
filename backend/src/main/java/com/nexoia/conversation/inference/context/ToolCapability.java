package com.nexoia.conversation.inference.context;

import java.util.List;

/**
 * The exact tools exposed to the model for this request. The model may only request a tool named
 * here; deterministic Nexo code still decides whether it executes.
 */
public record ToolCapability(List<String> exposedToolNames) {

    public static ToolCapability none() {
        return new ToolCapability(List.of());
    }
}
