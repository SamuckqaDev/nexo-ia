package com.nexoia.provider.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a provider stream cannot be started or fails midway. The public message never carries
 * the endpoint, host, or the provider's own error text.
 */
public class ProviderStreamException extends ApplicationException {

    public ProviderStreamException(Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "The model provider could not complete this request", cause);
    }
}
