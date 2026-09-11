package com.nexoia.provider.secret.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ProviderSecretConfigurationException extends ApplicationException {

    public ProviderSecretConfigurationException() {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "Remote provider credentials are unavailable until the server Secret Store is configured");
    }

    public ProviderSecretConfigurationException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "The server Secret Store could not process this provider credential", cause);
    }
}
