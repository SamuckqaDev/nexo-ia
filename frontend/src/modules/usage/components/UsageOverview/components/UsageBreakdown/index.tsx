import type { ReactElement } from "react";
import type { UsageLocationBreakdown, UsageModelBreakdown } from "../../../../types/usageTypes";
import {
  Detail,
  Empty,
  Fill,
  Grid,
  Name,
  Panel,
  PanelTitle,
  Row,
  RowHead,
  Track
} from "./styles";

type UsageBreakdownProps = {
  byModel: UsageModelBreakdown[];
  byLocation: UsageLocationBreakdown[];
};

const full = (value: number): string => new Intl.NumberFormat("en").format(value);

export function UsageBreakdown({ byModel, byLocation }: UsageBreakdownProps): ReactElement {
  const modelPeak: number = byModel.reduce(
    (peak: number, item: UsageModelBreakdown) => Math.max(peak, item.inputTokens + item.outputTokens),
    0
  );
  const locationPeak: number = byLocation.reduce(
    (peak: number, item: UsageLocationBreakdown) => Math.max(peak, item.totalTokens),
    0
  );

  return (
    <Grid>
      <Panel>
        <PanelTitle>By model</PanelTitle>
        {byModel.length === 0 ? (
          <Empty>No model has been used in this period.</Empty>
        ) : (
          byModel.map((item: UsageModelBreakdown) => {
            const total: number = item.inputTokens + item.outputTokens;
            return (
              <Row key={item.model}>
                <RowHead>
                  <Name title={item.model}>{item.model}</Name>
                  <Detail>{item.requests} req · {full(total)} tk</Detail>
                </RowHead>
                <Track><Fill $tone="primary" $ratio={modelPeak === 0 ? 0 : total / modelPeak} /></Track>
              </Row>
            );
          })
        )}
      </Panel>

      <Panel>
        <PanelTitle>By processing location</PanelTitle>
        {byLocation.length === 0 ? (
          <Empty>No processing location has been recorded yet.</Empty>
        ) : (
          byLocation.map((item: UsageLocationBreakdown) => (
            <Row key={item.processingLocation}>
              <RowHead>
                <Name>{item.processingLocation === "LOCAL" ? "On this machine" : "Remote provider"}</Name>
                <Detail>{item.requests} req · {full(item.totalTokens)} tk</Detail>
              </RowHead>
              <Track>
                <Fill $tone={item.processingLocation === "LOCAL" ? "primary" : "accent"}
                  $ratio={locationPeak === 0 ? 0 : item.totalTokens / locationPeak} />
              </Track>
            </Row>
          ))
        )}
      </Panel>
    </Grid>
  );
}
