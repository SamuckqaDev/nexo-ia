package com.nexoia.usage.dto;

public record UsageModelBreakdown(
        String model,
        long requests,
        long inputTokens,
        long outputTokens,
        Double averageLatencyMs) {}
