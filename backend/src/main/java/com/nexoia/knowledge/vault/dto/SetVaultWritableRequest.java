package com.nexoia.knowledge.vault.dto;

/** Toggles whether the assistant's governed {@code save_to_vault} tool may append to this Vault. */
public record SetVaultWritableRequest(boolean writable) {
}
