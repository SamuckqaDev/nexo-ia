package com.nexoia.knowledge.vault.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Thrown for a scope defined in the {@code VaultScope} contract but not yet backed by a real
 * authorization target — {@code PROJECT}, {@code TEAM}, and {@code ORGANIZATION} until their own
 * backend entities exist. See D-026.
 */
public class UnsupportedVaultScopeException extends ApplicationException {

    public UnsupportedVaultScopeException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "This Knowledge Vault scope is not supported yet");
    }
}
