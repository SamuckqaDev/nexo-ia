package com.nexoia.conversation.inference.context;

import java.util.List;

/** The Skills active for this request. Empty when none are applied. */
public record SkillCapability(List<String> activeSkillNames) {

    public static SkillCapability none() {
        return new SkillCapability(List.of());
    }
}
