package com.nexoia.conversation.inference.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised before inference when Agent work is requested but the provider has no tool model. */
public class AgentCapableModelUnavailableException extends ApplicationException {

    public AgentCapableModelUnavailableException() {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "No tool-capable model is available on the selected provider for this Agent request");
    }
}
