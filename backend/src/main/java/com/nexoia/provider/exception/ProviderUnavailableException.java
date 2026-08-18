package com.nexoia.provider.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ProviderUnavailableException extends ApplicationException {

    public ProviderUnavailableException() {
        super(HttpStatus.BAD_GATEWAY, "The Ollama provider is unavailable");
    }
}
