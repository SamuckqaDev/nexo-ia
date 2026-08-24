package com.nexoia.conversation.inference.tool;

import java.util.List;

/** Safe tool metadata resolved from callbacks already authorized for the current request. */
public record CapabilityInspectionResult(
        List<CapabilityInspectionTool> tools,
        String instruction) {

    public CapabilityInspectionResult {
        tools = List.copyOf(tools);
    }
}
