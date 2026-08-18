package com.nexoia.provider.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class ProviderConfigurationConflictException extends ConflictApplicationException {
    public ProviderConfigurationConflictException() {
        super("A provider with this endpoint is already configured for your account");
    }
}
