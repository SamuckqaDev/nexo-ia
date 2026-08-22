package com.nexoia.provider.springai;

import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolSession;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.nexoia.provider.service.ChatCompletionClient;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Streams Ollama chat completions through Spring AI's {@link ChatClient}, Advisor chain, and
 * {@link OllamaChatModel} instead of a hand-written {@code /api/chat} NDJSON parser.
 *
 * <p>Nexo still owns the boundary: it authorizes the endpoint/model, bounds the message history, maps
 * roles, drives cancellation, and translates any transport failure into a controlled business
 * exception that never leaks the provider's endpoint or error text. Spring AI owns the protocol.
 *
 * <p>Answer and reasoning are kept apart: Ollama exposes reasoning as a {@code thinking} property on
 * each streamed message, which is handed to {@code onThinking} and never appended to the persistable
 * answer.
 */
@Slf4j
@Service
public class SpringAiChatCompletionClient implements ChatCompletionClient {

    private static final String THINKING_KEY = "thinking";
    private static final String CANCELLED_REASON = "cancelled";

    private final SpringAiModelFactory modelFactory;
    private final SpringAiMessageMapper messageMapper;
    private final KnowledgeSearchToolFactory knowledgeToolFactory;

    public SpringAiChatCompletionClient(
            SpringAiModelFactory modelFactory,
            SpringAiMessageMapper messageMapper,
            KnowledgeSearchToolFactory knowledgeToolFactory) {
        this.modelFactory = modelFactory;
        this.messageMapper = messageMapper;
        this.knowledgeToolFactory = knowledgeToolFactory;
    }

    @Override
    public boolean supports(ProviderType providerType) {
        return providerType == ProviderType.OLLAMA;
    }

    @Override
    public ChatCompletionOutcome stream(
            ChatCompletionCommand command,
            Consumer<String> onThinking,
            Consumer<String> onToken,
            BooleanSupplier cancelled) {
        AtomicBoolean answerStarted = new AtomicBoolean(false);
        Consumer<String> trackedToken = delta -> {
            answerStarted.set(true);
            onToken.accept(delta);
        };

        try {
            return streamOnce(command, onThinking, trackedToken, cancelled);
        } catch (ProviderStreamException exception) {
            // Ollama rejects `think: true` for models that never advertise reasoning (for example
            // granite). A normal answer is still valid, so retry once without thinking — but only when
            // nothing was streamed yet, so an answer already shown to the user is never duplicated.
            if (!command.thinkingEnabled() || answerStarted.get() || !thinkingUnsupported(exception)) {
                throw exception;
            }
            log.warn("[NEXO-BACK][PROVIDER] Model does not accept thinking; retrying without it model={}",
                    command.model());
            return streamOnce(withoutThinking(command), onThinking, trackedToken, cancelled);
        }
    }

    private ChatCompletionOutcome streamOnce(
            ChatCompletionCommand command,
            Consumer<String> onThinking,
            Consumer<String> onToken,
            BooleanSupplier cancelled) {
        OllamaChatModel model =
                modelFactory.chatModel(command.endpoint(), command.model(), command.thinkingEnabled());
        List<Message> mapped = messageMapper.toSpringAi(command.messages());
        List<SystemMessage> systemContext = mapped.stream()
                .filter(SystemMessage.class::isInstance)
                .map(SystemMessage.class::cast)
                .toList();
        List<Message> conversation = mapped.stream()
                .filter(message -> !(message instanceof SystemMessage))
                .toList();
        Prompt prompt = new Prompt(conversation);
        ChatClient chatClient = ChatClient.builder(model).build();
        KnowledgeSearchToolSession toolSession = toolSession(command, cancelled);

        StringBuilder content = new StringBuilder();
        Integer inputTokens = null;
        Integer outputTokens = null;
        String finishReason = null;

        ChatClient.ChatClientRequestSpec request = chatClient.prompt(prompt)
                .advisors(new SpringAiContextAdvisor(systemContext));
        if (toolSession != null) {
            AtomicInteger toolRounds = new AtomicInteger();
            ToolCallingAdvisor toolAdvisor = ToolCallingAdvisor.builder()
                    .toolExecutionEligibilityChecker(response -> {
                        if (response == null || !response.hasToolCalls()) {
                            return false;
                        }
                        if (toolRounds.incrementAndGet() > KnowledgeSearchToolFactory.MAX_CALLS) {
                            throw new IllegalStateException("Governed tool call limit exceeded");
                        }
                        return true;
                    })
                    .build();
            request = request.advisors(toolAdvisor).toolCallbacks(toolSession.callback());
        }

        try (Stream<ChatResponse> responses = request
                .stream()
                .chatResponse()
                .toStream()) {
            Iterator<ChatResponse> iterator = responses.iterator();
            while (iterator.hasNext()) {
                // Consult cancellation before consuming each delta, matching the streaming contract:
                // closing the Stream disposes the underlying reactive subscription.
                if (cancelled.getAsBoolean()) {
                    return new ChatCompletionOutcome(
                            content.toString(), null, null, null, true, CANCELLED_REASON,
                            toolSession == null ? List.of() : toolSession.evidence());
                }

                ChatResponse response = iterator.next();
                Generation generation = response.getResult();
                if (generation != null) {
                    AssistantMessage message = generation.getOutput();
                    if (message.getMetadata().get(THINKING_KEY) instanceof String reasoning
                            && !reasoning.isEmpty()) {
                        onThinking.accept(reasoning);
                    }
                    String delta = message.getText();
                    if (delta != null && !delta.isEmpty()) {
                        content.append(delta);
                        onToken.accept(delta);
                    }
                    if (generation.getMetadata() != null && generation.getMetadata().getFinishReason() != null) {
                        finishReason = generation.getMetadata().getFinishReason();
                    }
                }

                Usage usage = response.getMetadata().getUsage();
                if (usage != null) {
                    if (usage.getPromptTokens() != null) {
                        inputTokens = usage.getPromptTokens();
                    }
                    if (usage.getCompletionTokens() != null) {
                        outputTokens = usage.getCompletionTokens();
                    }
                }
            }
        } catch (ProviderStreamException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][PROVIDER] Ollama stream failed model={} reason={}",
                    command.model(), exception.getClass().getSimpleName());
            throw new ProviderStreamException(exception);
        }

        TokenSource tokenSource =
                inputTokens == null && outputTokens == null ? null : TokenSource.PROVIDER;
        return new ChatCompletionOutcome(
                content.toString(), inputTokens, outputTokens, tokenSource, false, finishReason,
                toolSession == null ? List.of() : toolSession.evidence());
    }

    private KnowledgeSearchToolSession toolSession(
            ChatCompletionCommand command,
            BooleanSupplier cancelled) {
        if (command.mode() != ConversationMode.AGENT
                || command.knowledgeToolScope() == null
                || !command.knowledgeToolScope().available()) {
            return null;
        }
        return knowledgeToolFactory.open(
                command.knowledgeToolScope(), command.toolExecutionObserver(), cancelled);
    }

    private ChatCompletionCommand withoutThinking(ChatCompletionCommand command) {
        return new ChatCompletionCommand(
                command.providerType(), command.endpoint(), command.model(), command.messages(), false,
                command.mode(), command.knowledgeToolScope(), command.toolExecutionObserver());
    }

    private boolean thinkingUnsupported(ProviderStreamException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof WebClientResponseException.BadRequest badRequest) {
                String body = badRequest.getResponseBodyAsString().toLowerCase();
                return body.contains("think") || body.contains("thinking");
            }
            cause = cause.getCause();
        }
        return false;
    }
}
