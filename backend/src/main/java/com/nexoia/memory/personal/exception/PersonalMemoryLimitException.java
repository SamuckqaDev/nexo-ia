package com.nexoia.memory.personal.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class PersonalMemoryLimitException extends ApplicationException {

    public PersonalMemoryLimitException() {
        super(HttpStatus.CONFLICT, "Personal memory limit reached; remove an existing memory first");
    }
}
