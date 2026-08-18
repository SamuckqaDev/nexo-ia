package com.nexoia.provider.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ProviderConfigurationNotFoundException extends ApplicationException {
    public ProviderConfigurationNotFoundException() { super(HttpStatus.NOT_FOUND, "Provider configuration not found"); }
}
