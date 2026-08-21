package com.nexoia.knowledge.ingestion.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class SourceTooLargeException extends ApplicationException {

    public SourceTooLargeException() {
        super(HttpStatus.PAYLOAD_TOO_LARGE, "The source file exceeds the maximum upload size");
    }
}
