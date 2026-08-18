package com.nexoia.provider.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a registered endpoint resolves to an address the provider type may not reach. The
 * message stays generic so an unauthorized caller learns nothing about the internal network.
 */
public class ProviderEndpointNotAllowedException extends ApplicationException {

    public ProviderEndpointNotAllowedException() {
        super(HttpStatus.BAD_REQUEST, "This provider type cannot use the configured endpoint");
    }
}
