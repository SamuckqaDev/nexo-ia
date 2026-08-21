package com.nexoia.knowledge.vault.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class VaultNotFoundException extends ApplicationException {

    public VaultNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Knowledge Vault not found");
    }
}
