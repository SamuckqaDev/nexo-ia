package com.nexoia.memory.personal.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class PersonalMemoryNotFoundException extends ApplicationException {

    public PersonalMemoryNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Personal memory not found");
    }
}
