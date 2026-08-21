package com.nexoia.knowledge.ingestion.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class SourceNotFoundException extends ApplicationException {

    public SourceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Knowledge source not found");
    }
}
