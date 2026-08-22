package com.nexoia.provider.dto;

import java.time.Instant;
import java.util.UUID;

/** Safe start evidence: arguments are represented by a digest, never by raw private text. */
public record ToolExecutionStarted(
        UUID executionId,
        String toolName,
        String argumentsDigest,
        Instant startedAt) {}
