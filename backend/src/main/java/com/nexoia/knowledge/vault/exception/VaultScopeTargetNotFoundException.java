package com.nexoia.knowledge.vault.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class VaultScopeTargetNotFoundException extends ApplicationException {

    public VaultScopeTargetNotFoundException() {
        super(HttpStatus.NOT_FOUND, "The selected scope target was not found");
    }
}
