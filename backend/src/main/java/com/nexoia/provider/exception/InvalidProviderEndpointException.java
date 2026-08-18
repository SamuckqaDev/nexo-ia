package com.nexoia.provider.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidProviderEndpointException extends ApplicationException {
    public InvalidProviderEndpointException() {
        super(HttpStatus.BAD_REQUEST, "Provider endpoint must be a valid absolute HTTP or HTTPS URL");
    }
}
