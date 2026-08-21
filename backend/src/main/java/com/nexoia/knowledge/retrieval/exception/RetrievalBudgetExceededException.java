package com.nexoia.knowledge.retrieval.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RetrievalBudgetExceededException extends ApplicationException {

    public RetrievalBudgetExceededException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Too many Knowledge Vaults were selected for this request");
    }
}
