package com.nexoia.usage.dto;

import java.time.LocalDate;

public record UsageDailyPoint(
        LocalDate date,
        long requests,
        long inputTokens,
        long outputTokens) {}
