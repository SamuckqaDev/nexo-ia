package com.nexoia.conversation.inference.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.conversation.inference.prompt.PromptResourceService;
import com.nexoia.provider.model.ProcessingLocation;
import java.util.List;
import org.junit.jupiter.api.Test;

class CapabilityEnvelopeRendererTest {

    private final CapabilityEnvelopeRenderer renderer =
            new CapabilityEnvelopeRenderer(new PromptResourceService());

    @Test
    void statesNoKnowledgeWhenNothingWasSelectedOrRetrieved() {
        String rendered = renderer.render(envelope(KnowledgeCapability.none(), ToolCapability.none()));

        assertThat(rendered).contains("Knowledge Vaults selected: none");
        assertThat(rendered).contains("Knowledge search status: not_requested");
        assertThat(rendered).contains("Sources retrieved: 0");
        assertThat(rendered).contains("No Knowledge Vault search was requested");
        assertThat(rendered).contains("Tools available this request: none");
        assertThat(rendered).contains("MCP tools enabled: none");
    }

    @Test
    void statesTheSelectedVaultsAndRetrievedSourcesWhenPresent() {
        KnowledgeCapability knowledge = new KnowledgeCapability(
                List.of("Nexo Knowledge Base"), 1, KnowledgeSearchStatus.COMPLETED, 2,
                List.of(new ContextSourceSummary("Nexo Knowledge Base", "Nexo Principles", 1)));

        String rendered = renderer.render(envelope(knowledge, new ToolCapability(List.of("search_knowledge"))));

        assertThat(rendered).contains("Knowledge Vaults selected: 1 (Nexo Knowledge Base)");
        assertThat(rendered).contains("Knowledge search status: completed");
        assertThat(rendered).contains("Sources retrieved: 2");
        assertThat(rendered).doesNotContain("no relevant sources");
        assertThat(rendered).contains("Tools available this request: search_knowledge");
    }

    @Test
    void tellsAgentHowToUseOnDemandKnowledgeAndEnabledMcpTools() {
        KnowledgeCapability knowledge = new KnowledgeCapability(
                List.of("Nexo Knowledge Base"), 1, KnowledgeSearchStatus.AVAILABLE_ON_DEMAND, 0,
                List.of());

        String rendered = renderer.render(envelope(knowledge,
                new ToolCapability(List.of("update_plan", "search_knowledge", "mcp_12345678_fetch"))));

        assertThat(rendered).contains("Knowledge search status: available_on_demand");
        assertThat(rendered).contains("available through the `search_knowledge` tool");
        assertThat(rendered).contains("MCP tools enabled: mcp_12345678_fetch");
        assertThat(rendered).contains("callable external MCP tools");
    }

    private ModelContextEnvelope envelope(KnowledgeCapability knowledge, ToolCapability tools) {
        return new ModelContextEnvelope("samuckqadev", "chat",
                new CapabilityManifest("qwen3:8b", ProcessingLocation.LOCAL,
                        knowledge, WorkspaceCapability.none(), SkillCapability.none(), tools));
    }
}
