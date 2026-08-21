package com.nexoia.knowledge.embedding.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when no enabled Ollama provider is registered, or the embedding call itself fails. Callers
 * must resolve this to an explicit "no knowledge found" outcome — never an invented answer and never a
 * lexical fallback in this release. See D-026.
 */
public class EmbeddingProviderUnavailableException extends ApplicationException {

    public EmbeddingProviderUnavailableException() {
        super(HttpStatus.BAD_GATEWAY, "The embedding provider is unavailable");
    }
}
