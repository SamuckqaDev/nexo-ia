package com.nexoia.device.runtime.dto;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record RuntimeEnvelope(
        String protocol,
        String type,
        UUID id,
        UUID runId,
        UUID taskId,
        Long sequence,
        Instant timestamp,
        String method,
        JsonNode payload,
        RuntimeError error) {}
