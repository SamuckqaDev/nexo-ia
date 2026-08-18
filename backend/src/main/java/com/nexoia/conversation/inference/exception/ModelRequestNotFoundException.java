package com.nexoia.conversation.inference.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ModelRequestNotFoundException extends ApplicationException {

    public ModelRequestNotFoundException() {
        super(HttpStatus.NOT_FOUND, "No model request in progress for this conversation");
    }
}
