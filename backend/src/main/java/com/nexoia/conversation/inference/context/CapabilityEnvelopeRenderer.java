package com.nexoia.conversation.inference.context;

import com.nexoia.conversation.inference.prompt.PromptResource;
import com.nexoia.conversation.inference.prompt.PromptResourceService;
import com.nexoia.permission.model.ContentArea;
import com.nexoia.permission.model.ContentMatrix;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Renders a {@link ModelContextEnvelope} into a deterministic system message. The framing sentence
 * comes from the editable {@code capability-envelope.md} resource; the facts are laid out in a fixed
 * order so the same request always produces the same prompt.
 */
@Service
@RequiredArgsConstructor
public class CapabilityEnvelopeRenderer {

    private final PromptResourceService prompts;

    public String render(ModelContextEnvelope envelope) {
        CapabilityManifest manifest = envelope.manifest();
        KnowledgeCapability knowledge = manifest.knowledge();

        PermissionCapability permission = manifest.permission();

        StringBuilder builder = new StringBuilder(prompts.get(PromptResource.CAPABILITY_ENVELOPE));
        builder.append("\n\n");
        line(builder, "User", envelope.username() == null ? "unauthenticated" : envelope.username());
        line(builder, "Conversation mode", envelope.conversationMode());
        line(builder, "Model", manifest.providerModel() + " (processing: "
                + manifest.processingLocation().name().toLowerCase() + ")");
        line(builder, "Permission profile",
                permission.profileName() + " (capability level " + permission.level().label() + ")");
        line(builder, "Content policy (by area)", contentPolicy(permission.contentMatrix()));
        line(builder, "Knowledge Vaults selected",
                knowledge.selectedVaultCount() == 0
                        ? "none"
                        : knowledge.selectedVaultCount() + " (" + join(knowledge.selectedVaultNames()) + ")");
        line(builder, "Knowledge search status", knowledge.searchStatus().name().toLowerCase());
        line(builder, "Sources retrieved", String.valueOf(knowledge.sourcesRetrieved()));
        line(builder, "Workspace", manifest.workspace().present()
                ? manifest.workspace().name() + " (server-side access: " + manifest.workspace().serverSideAccess() + ")"
                : "none attached");
        line(builder, "Active Skills", manifest.skills().activeSkillNames().isEmpty()
                ? "none" : join(manifest.skills().activeSkillNames()));
        line(builder, "Tools available this request", manifest.tools().exposedToolNames().isEmpty()
                ? "none" : join(manifest.tools().exposedToolNames()));

        List<String> mcpTools = manifest.tools().exposedToolNames().stream()
                .filter(tool -> tool.startsWith("mcp_"))
                .toList();
        line(builder, "MCP connection status", mcpTools.isEmpty()
                ? "no enabled MCP tools are connected to this request"
                : mcpTools.size() + " enabled tool(s)");
        line(builder, "MCP tools enabled", mcpTools.isEmpty() ? "none" : join(mcpTools));

        if (knowledge.sourcesRetrieved() == 0) {
            builder.append('\n').append(noKnowledgeInstruction(knowledge.searchStatus()));
        }
        if (mcpTools.isEmpty()) {
            builder.append('\n').append("No external MCP tool is connected to this request. If the user asks "
                    + "for web search or another external action, state this limitation and direct them to "
                    + "enable a suitable server in the MCP Hub. You cannot connect or enable one yourself.");
        } else {
            builder.append('\n').append("Tools whose names start with `mcp_` are callable external MCP tools "
                    + "explicitly enabled by the user. Use the exact listed tool name when it directly helps "
                    + "the request, before claiming that external access is unavailable, and report a result "
                    + "only after the tool returns evidence.");
        }

        if (!permission.lockedCapabilities().isEmpty()) {
            builder.append('\n').append("You do not have these capabilities at this level: ")
                    .append(join(permission.lockedCapabilities()))
                    .append(". Say so plainly if asked; you cannot raise your own level, enable a tool, or "
                            + "grant yourself access. The user unlocks a capability by raising the permission "
                            + "profile or approving a specific action.");
        }

        return builder.toString().strip();
    }

    private String contentPolicy(ContentMatrix matrix) {
        StringBuilder areas = new StringBuilder();
        for (ContentArea area : ContentArea.values()) {
            if (!areas.isEmpty()) {
                areas.append(", ");
            }
            areas.append(area.name().toLowerCase().replace('_', ' '))
                    .append(" = ").append(matrix.allowance(area).name().toLowerCase());
        }
        return areas + ". FULL = generate freely (lawful); PARTIAL = explain factually but do not generate "
                + "graphic material; BLOCK = refuse, not enabled for your profile. This is a separate axis "
                + "from your capabilities and never blocks a topic beyond these areas. Refuse only genuinely "
                + "illegal or serious-harm content, regardless of the above.";
    }

    private void line(StringBuilder builder, String label, String value) {
        builder.append("- ").append(label).append(": ").append(value).append('\n');
    }

    private String join(List<String> values) {
        return String.join(", ", values);
    }

    private String noKnowledgeInstruction(KnowledgeSearchStatus status) {
        return switch (status) {
            case NOT_REQUESTED -> "No Knowledge Vault search was requested. Do not claim you searched, "
                    + "found, or used internal knowledge.";
            case AVAILABLE_ON_DEMAND -> "The selected Knowledge Vaults are available through the "
                    + "`search_knowledge` tool. Before answering a request that may depend on the user's "
                    + "knowledge, call that tool with a focused query and use only returned evidence.";
            case COMPLETED -> "The Knowledge Vault search completed but returned no relevant sources. "
                    + "You may state that no relevant source was found; do not invent or attribute content.";
            case UNAVAILABLE -> "Knowledge Vault search was unavailable. State that limitation plainly and "
                    + "do not claim you searched, found, or used internal knowledge.";
        };
    }
}
