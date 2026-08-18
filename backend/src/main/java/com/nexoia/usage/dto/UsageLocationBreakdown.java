package com.nexoia.usage.dto;

import com.nexoia.provider.model.ProcessingLocation;

public record UsageLocationBreakdown(
        ProcessingLocation processingLocation,
        long requests,
        long totalTokens) {}
