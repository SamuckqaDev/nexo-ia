package com.nexoia.conversation.inference.context;

import com.nexoia.provider.model.ProcessingLocation;

/**
 * The full set of capabilities and context resolved for one model request, aggregated from
 * already-authorized state. It is the single source of truth rendered into the model-facing envelope.
 */
public record CapabilityManifest(
        String providerModel,
        ProcessingLocation processingLocation,
        KnowledgeCapability knowledge,
        WorkspaceCapability workspace,
        SkillCapability skills,
        ToolCapability tools) {}
