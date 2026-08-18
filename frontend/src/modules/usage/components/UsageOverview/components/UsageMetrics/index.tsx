import type { ReactElement } from "react";
import type { UsageTotals } from "../../../../types/usageTypes";
import { Grid, Label, Note, StatusPill, StatusRow, Tile, Value } from "./styles";

type UsageMetricsProps = {
  totals: UsageTotals;
};

const compact = (value: number): string =>
  new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(value);

const full = (value: number): string => new Intl.NumberFormat("en").format(value);

export function UsageMetrics({ totals }: UsageMetricsProps): ReactElement {
  const averageLatency: string = totals.averageLatencyMs === null
    ? "—"
    : `${(totals.averageLatencyMs / 1000).toFixed(2)}s`;

  return (
    <Grid>
      <Tile>
        <Label>Requests</Label>
        <Value>{full(totals.requests)}</Value>
        <StatusRow>
          <StatusPill $tone="good">{totals.completed} completed</StatusPill>
          {totals.cancelled > 0 && <StatusPill $tone="warning">{totals.cancelled} cancelled</StatusPill>}
          {totals.failed > 0 && <StatusPill $tone="danger">{totals.failed} failed</StatusPill>}
        </StatusRow>
      </Tile>

      <Tile>
        <Label>Total tokens</Label>
        <Value>{compact(totals.totalTokens)}</Value>
        <Note>{full(totals.totalTokens)} in this period</Note>
      </Tile>

      <Tile>
        <Label>Input tokens</Label>
        <Value>{compact(totals.inputTokens)}</Value>
        <Note>Prompt and context</Note>
      </Tile>

      <Tile>
        <Label>Output tokens</Label>
        <Value>{compact(totals.outputTokens)}</Value>
        <Note>Model responses</Note>
      </Tile>

      <Tile>
        <Label>Average latency</Label>
        <Value>{averageLatency}</Value>
        <Note>Across completed and cancelled runs</Note>
      </Tile>

      <Tile>
        <Label>Estimated counts</Label>
        <Value>{full(totals.estimatedTokenRequests)}</Value>
        <Note>
          {totals.estimatedTokenRequests === 0
            ? "All counts provider-reported"
            : "Requests without provider token counts"}
        </Note>
      </Tile>
    </Grid>
  );
}
