package com.nexoia.knowledge.vault.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when the assistant tries to append knowledge to a Vault the owner has not made writable. */
public class VaultNotWritableException extends ApplicationException {

    public VaultNotWritableException() {
        super(HttpStatus.CONFLICT, "This Knowledge Vault is not writable");
    }
}
