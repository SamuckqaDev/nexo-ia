package com.nexoia.conversation.inference.service;

import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.conversation.inference.context.CapabilityEnvelopeRenderer;
import com.nexoia.conversation.inference.context.ModelContextEnvelope;
import com.nexoia.conversation.inference.prompt.PromptResource;
import com.nexoia.conversation.inference.prompt.PromptResourceService;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.provider.dto.ChatCompletionMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the message history sent to a model, bounded by an explicit token budget.
 *
 * <p>Only the requested conversation is read, and only messages that actually became part of it:
 * a failed generation is excluded, while a cancelled one keeps the partial text the user saw. The
 * assistant's identity and rules come from editable Markdown resources, not from Java text.
 */
@Service
@RequiredArgsConstructor
public class ConversationContextAssembler {

    private static final Set<MessageStatus> USABLE =
            Set.of(MessageStatus.COMPLETED, MessageStatus.CANCELLED);

    private final ConversationMessageRepository messages;
    private final ConversationContextProperties properties;
    private final PromptResourceService prompts;
    private final CapabilityEnvelopeRenderer envelopeRenderer;

    public List<ChatCompletionMessage> assemble(UUID conversationId) {
        return assemble(conversationId, null, List.of(), null);
    }

    public List<ChatCompletionMessage> assemble(UUID conversationId, String username) {
        return assemble(conversationId, username, List.of(), null);
    }

    public List<ChatCompletionMessage> assemble(
            UUID conversationId, String username, List<CitationResponse> citations) {
        return assemble(conversationId, username, citations, null);
    }

    /**
     * Assembles the full system context plus bounded history. The leading system messages are, in
     * order: the assistant identity and rules, the per-request capability envelope (when provided),
     * and the retrieved Knowledge Vault excerpts (when any) — all explicitly framed as authoritative
     * governance or untrusted reference. See D-026.
     */
    public List<ChatCompletionMessage> assemble(
            UUID conversationId, String username, List<CitationResponse> citations,
            ModelContextEnvelope envelope) {
        String identity = identityFor(username, envelope);
        String envelopeMessage = envelope == null ? null : envelopeRenderer.render(envelope);
        String knowledgeMessage = citations == null || citations.isEmpty() ? null : retrievedContext(citations);

        int reserved = properties.estimateTokens(identity)
                + estimate(envelopeMessage) + estimate(knowledgeMessage);
        List<ChatCompletionMessage> selected = boundedHistory(conversationId, reserved);

        List<ChatCompletionMessage> context = new ArrayList<>(selected.size() + 3);
        context.add(new ChatCompletionMessage("system", identity));
        if (envelopeMessage != null) {
            context.add(new ChatCompletionMessage("system", envelopeMessage));
        }
        if (knowledgeMessage != null) {
            context.add(new ChatCompletionMessage("system", knowledgeMessage));
        }
        context.addAll(selected.reversed());
        return context;
    }

    private List<ChatCompletionMessage> boundedHistory(UUID conversationId, int reserved) {
        List<ConversationMessage> history = messages.findContextHistory(conversationId, USABLE);
        List<ChatCompletionMessage> selected = new ArrayList<>();
        int budget = properties.tokenBudget();
        int used = reserved;

        // Walk backwards so the most recent turns survive a tight budget.
        for (int index = history.size() - 1; index >= 0; index--) {
            ConversationMessage message = history.get(index);
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }

            int cost = properties.estimateTokens(message.getContent());
            boolean isNewestMessage = selected.isEmpty();

            // The newest message is the request itself: dropping it would send an empty prompt.
            if (!isNewestMessage && used + cost > budget) {
                break;
            }

            selected.add(new ChatCompletionMessage(role(message), message.getContent()));
            used += cost;
        }

        return selected;
    }

    private int estimate(String text) {
        return text == null ? 0 : properties.estimateTokens(text);
    }

    private String identityFor(String username, ModelContextEnvelope envelope) {
        String base = prompts.get(PromptResource.IDENTITY) + "\n\n" + prompts.get(PromptResource.RULES);
        if (envelope != null && "agent".equalsIgnoreCase(envelope.conversationMode())) {
            base += "\n\n" + prompts.get(PromptResource.AGENT_RULES);
        }
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "\n\nThe authenticated user's username is `" + username.trim()
                + "`. Address the user by this username naturally when it fits the conversation.";
    }

    private String retrievedContext(List<CitationResponse> citations) {
        StringBuilder builder = new StringBuilder(prompts.get(PromptResource.KNOWLEDGE_CONTEXT));
        builder.append("\n\n");
        for (CitationResponse citation : citations) {
            builder.append("[").append(citation.vaultName()).append('/')
                    .append(citation.sourceDisplayName()).append('#').append(citation.chunkOrdinal())
                    .append("]\n").append(citation.excerpt()).append("\n\n");
        }

        return builder.toString().strip();
    }

    private String role(ConversationMessage message) {
        return message.getRole() == ConversationRole.USER ? "user" : "assistant";
    }
}
