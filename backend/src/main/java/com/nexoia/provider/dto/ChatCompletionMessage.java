package com.nexoia.provider.dto;

/**
 * One message handed to a chat model. Roles use the vendor-neutral values shared by the supported
 * providers.
 */
public record ChatCompletionMessage(String role, String content) {}
