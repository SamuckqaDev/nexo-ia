package com.nexoia.conversation.inference.tool;

/** One model-safe capability description; it never contains ownership ids or connection secrets. */
public record CapabilityInspectionTool(String name, String description) {}
