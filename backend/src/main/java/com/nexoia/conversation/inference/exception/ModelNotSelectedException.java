package com.nexoia.conversation.inference.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ModelNotSelectedException extends ApplicationException {

    public ModelNotSelectedException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Select a provider and model for this conversation first");
    }
}
