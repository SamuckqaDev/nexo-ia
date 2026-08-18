import { useState, type ReactElement } from "react";
import { Button } from "../../../../shared/components/Button";
import { Loading } from "../../../../shared/components/Loading";
import { useUsage } from "../../hooks/useUsage";
import type { UsagePeriod } from "../../types/usageTypes";
import { UsageBreakdown } from "./components/UsageBreakdown";
import { UsageChart } from "./components/UsageChart";
import { UsageMetrics } from "./components/UsageMetrics";
import { Failure, Overview, PeriodButton, Periods, Scope, Toolbar } from "./styles";

const periods: Array<{ id: UsagePeriod; label: string }> = [
  { id: "LAST_24_HOURS", label: "24 hours" },
  { id: "LAST_7_DAYS", label: "7 days" },
  { id: "LAST_30_DAYS", label: "30 days" },
  { id: "ALL_TIME", label: "All time" }
];

export function UsageOverview(): ReactElement {
  const [period, setPeriod] = useState<UsagePeriod>("LAST_7_DAYS");
  const usage = useUsage(period);

  return (
    <Overview>
      <Toolbar>
        <Periods role="group" aria-label="Usage period">
          {periods.map((item: { id: UsagePeriod; label: string }) => (
            <PeriodButton
              key={item.id}
              type="button"
              $active={period === item.id}
              aria-pressed={period === item.id}
              onClick={(): void => setPeriod(item.id)}
            >
              {item.label}
            </PeriodButton>
          ))}
        </Periods>
        <Scope>Only your own activity. Pricing and team totals are not part of this release.</Scope>
      </Toolbar>

      {usage.isLoading && <Loading label="Adding up your usage…" />}

      {usage.isError && (
        <Failure>
          <span>We could not load your usage.</span>
          <Button type="button" variant="outline" onClick={(): void => { usage.refetch(); }}>
            Try again
          </Button>
        </Failure>
      )}

      {usage.data && (
        <>
          <UsageMetrics totals={usage.data.totals} />
          <UsageChart daily={usage.data.daily} />
          <UsageBreakdown
            byModel={usage.data.byModel}
            byLocation={usage.data.byProcessingLocation}
          />
        </>
      )}
    </Overview>
  );
}
