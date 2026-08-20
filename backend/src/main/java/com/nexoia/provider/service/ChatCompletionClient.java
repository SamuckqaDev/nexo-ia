package com.nexoia.provider.service;

import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.model.ProviderType;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The provider boundary for streamed chat inference.
 *
 * <p>This interface exists because Nexo IA must support several real provider protocols — local and
 * home-server Ollama first, then OpenAI, Gemini, Anthropic, and OpenAI-compatible servers — without
 * conversation rules depending on any one vendor. It is a genuine adapter contract, not a
 * same-purpose interface placed in front of a single implementation by habit.
 */
public interface ChatCompletionClient {

    boolean supports(ProviderType providerType);

    /**
     * Streams a completion, handing provider reasoning to {@code onThinking} and final-answer deltas
     * to {@code onToken}. Reasoning must not be included in the returned outcome content.
     *
     * <p>Implementations must consult {@code cancelled} between deltas and stop reading when it turns
     * true, returning the partial content with {@code cancelled} set on the outcome. They must never
     * fall back to another endpoint or provider.
     */
    ChatCompletionOutcome stream(
            ChatCompletionCommand command,
            Consumer<String> onThinking,
            Consumer<String> onToken,
            BooleanSupplier cancelled);
}
