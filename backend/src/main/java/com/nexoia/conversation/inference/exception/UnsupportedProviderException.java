package com.nexoia.conversation.inference.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Raised when no adapter implements the configured provider type. Nexo IA fails explicitly instead
 * of silently falling back to another provider.
 */
public class UnsupportedProviderException extends ApplicationException {

    public UnsupportedProviderException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "This provider type cannot run chat inference yet");
    }
}
