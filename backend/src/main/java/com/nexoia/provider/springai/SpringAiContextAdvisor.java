package com.nexoia.provider.springai;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

/**
 * Applies Nexo's already-authorized identity, capability, and knowledge messages through the
 * Spring AI Advisor chain. The advisor never performs authorization or retrieval itself: those
 * decisions happen before provider execution and the resulting bounded messages are immutable for
 * this request.
 */
public class SpringAiContextAdvisor implements CallAdvisor, StreamAdvisor {

    static final String SYSTEM_MESSAGE_COUNT = "nexo.context.system-message-count";

    private final List<SystemMessage> systemMessages;

    public SpringAiContextAdvisor(List<SystemMessage> systemMessages) {
        this.systemMessages = List.copyOf(systemMessages);
    }

    @Override
    public ChatClientResponse adviseCall(
            ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(augment(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(augment(request));
    }

    @Override
    public String getName() {
        return SpringAiContextAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private ChatClientRequest augment(ChatClientRequest request) {
        List<Message> instructions = new ArrayList<>(
                systemMessages.size() + request.prompt().getInstructions().size());
        instructions.addAll(systemMessages);
        instructions.addAll(request.prompt().getInstructions());

        Prompt prompt = new Prompt(instructions, request.prompt().getOptions());
        return request.mutate()
                .prompt(prompt)
                .context(SYSTEM_MESSAGE_COUNT, systemMessages.size())
                .build();
    }
}
