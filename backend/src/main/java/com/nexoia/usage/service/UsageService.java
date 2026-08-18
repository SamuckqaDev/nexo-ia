package com.nexoia.usage.service;

import com.nexoia.usage.dto.UsageDailyPoint;
import com.nexoia.usage.dto.UsageSummaryResponse;
import com.nexoia.usage.dto.UsageTotals;
import com.nexoia.usage.model.UsagePeriod;
import com.nexoia.usage.repository.UsageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reports the authenticated member's own model consumption.
 *
 * <p>Only personal usage exists in release 0.1. Organization summaries require the organization
 * entity, which is a later identity increment, and pricing is explicitly out of scope.
 */
@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UsageSummaryResponse summary(UUID userId, UsagePeriod period) {
        Instant now = clock.instant();
        Instant from = period.startingFrom(now);

        return new UsageSummaryResponse(
                period,
                from,
                now,
                totals(userId, from),
                daily(userId, from),
                repository.byModel(userId, from),
                repository.byProcessingLocation(userId, from));
    }

    private UsageTotals totals(UUID userId, Instant from) {
        UsageTotals totals = repository.totals(userId, from);

        // An empty window still aggregates to a row, but its sums are null.
        return totals == null
                ? new UsageTotals(0, 0, 0, 0, 0, 0, 0, null, 0)
                : totals;
    }

    /**
     * Groups requests by calendar day in UTC. Interface layers convert to the viewer's timezone; the
     * stored instants stay in UTC.
     */
    private List<UsageDailyPoint> daily(UUID userId, Instant from) {
        Map<LocalDate, long[]> byDay = new TreeMap<>();

        for (Object[] row : repository.dailyRows(userId, from)) {
            LocalDate day = ((Instant) row[0]).atZone(ZoneOffset.UTC).toLocalDate();
            long[] counters = byDay.computeIfAbsent(day, key -> new long[3]);
            counters[0]++;
            counters[1] += row[1] == null ? 0L : ((Number) row[1]).longValue();
            counters[2] += row[2] == null ? 0L : ((Number) row[2]).longValue();
        }

        return byDay.entrySet().stream()
                .map(entry -> new UsageDailyPoint(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();
    }
}
