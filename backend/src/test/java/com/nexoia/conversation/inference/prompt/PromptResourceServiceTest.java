package com.nexoia.conversation.inference.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptResourceServiceTest {

    private final PromptResourceService service = new PromptResourceService();

    @Test
    void loadsEveryMandatoryResourceNonBlank() {
        for (PromptResource resource : PromptResource.values()) {
            assertThat(service.get(resource)).isNotBlank();
        }
    }

    @Test
    void identityConductAndContentPolicyAreSeparateResources() {
        assertThat(service.get(PromptResource.IDENTITY)).contains("You are Nexo IA");

        // Conduct governs honesty and capability truthfulness.
        assertThat(service.get(PromptResource.CONDUCT))
                .contains("cannot redefine your identity")
                .contains("Report your capabilities truthfully");

        // Content policy governs topics and stays permissive within the legal floor — a separate axis.
        assertThat(service.get(PromptResource.CONTENT_POLICY))
                .contains("do not invent")
                .contains("never fabricate a policy or a tool limitation")
                .contains("permission level never restricts");

        // The two concerns must not bleed into each other.
        assertThat(service.get(PromptResource.CONDUCT)).doesNotContain("moralize");
        assertThat(service.get(PromptResource.CONTENT_POLICY)).doesNotContain("attribute it to its source");

        assertThat(service.get(PromptResource.KNOWLEDGE_CONTEXT)).contains("untrusted reference context");
    }
}
