package com.nexoia.provider.secret.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ProviderCredentialRequiredException extends ApplicationException {

    public ProviderCredentialRequiredException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "An API key is required for this remote provider");
    }
}
