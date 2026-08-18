package com.nexoia.usage.dto;

import com.nexoia.usage.model.UsagePeriod;
import java.time.Instant;
import java.util.List;

/** Personal usage for the authenticated member over one reporting window. */
public record UsageSummaryResponse(
        UsagePeriod period,
        Instant from,
        Instant to,
        UsageTotals totals,
        List<UsageDailyPoint> daily,
        List<UsageModelBreakdown> byModel,
        List<UsageLocationBreakdown> byProcessingLocation) {}
