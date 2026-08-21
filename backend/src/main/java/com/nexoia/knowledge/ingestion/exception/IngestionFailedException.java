package com.nexoia.knowledge.ingestion.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class IngestionFailedException extends ApplicationException {

    public IngestionFailedException() {
        super(HttpStatus.BAD_GATEWAY, "Ingestion failed for this source");
    }
}
