package com.nexoia.usage.dto;

/**
 * Aggregate counters for the reporting window.
 *
 * <p>{@code estimatedTokenRequests} is reported separately so an estimate is never presented as a
 * provider measurement. Cost is intentionally absent: pricing and budgets are outside release 0.1.
 *
 * <p>The average latency is a {@code Double} because that is what SQL {@code avg} returns, and it
 * is null for a window with no recorded request.
 */
public record UsageTotals(
        long requests,
        long completed,
        long cancelled,
        long failed,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        Double averageLatencyMs,
        long estimatedTokenRequests) {}
