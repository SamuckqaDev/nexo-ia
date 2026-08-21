package com.nexoia.knowledge.ingestion.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Thrown only when the upload cannot be processed at all (empty file, no readable name). A
 * recognized-but-not-yet-ingestible MIME type (for example PDF) is not rejected — it is stored with
 * {@code SourceStatus.UNSUPPORTED} instead, per D-026.
 */
public class UnsupportedSourceTypeException extends ApplicationException {

    public UnsupportedSourceTypeException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "This file cannot be registered as a knowledge source");
    }
}
