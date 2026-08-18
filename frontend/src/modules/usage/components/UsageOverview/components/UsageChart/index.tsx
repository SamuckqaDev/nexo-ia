import { useMemo, useState, type ReactElement } from "react";
import type { UsageDailyPoint } from "../../../../types/usageTypes";
import {
  AxisLabel,
  Bar,
  Caption,
  CaptionTitle,
  Empty,
  Figure,
  Legend,
  LegendItem,
  Plot,
  Svg,
  Tooltip,
  TooltipDate,
  TooltipRow
} from "./styles";

type UsageChartProps = {
  daily: UsageDailyPoint[];
};

type HoverState = { index: number; x: number; y: number };

const WIDTH = 720;
const HEIGHT = 220;
const PADDING = { top: 16, right: 12, bottom: 26, left: 44 };
const PLOT_WIDTH = WIDTH - PADDING.left - PADDING.right;
const PLOT_HEIGHT = HEIGHT - PADDING.top - PADDING.bottom;
const SEGMENT_GAP = 2;

const compact = (value: number): string =>
  new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(value);

const full = (value: number): string => new Intl.NumberFormat("en").format(value);

const shortDate = (iso: string): string =>
  new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(new Date(`${iso}T00:00:00`));

export function UsageChart({ daily }: UsageChartProps): ReactElement {
  const [hover, setHover] = useState<HoverState | null>(null);

  const scale = useMemo(() => {
    const max = daily.reduce(
      (peak: number, point: UsageDailyPoint) => Math.max(peak, point.inputTokens + point.outputTokens),
      0
    );
    return max === 0 ? 1 : max;
  }, [daily]);

  if (daily.length === 0) {
    return (
      <Figure>
        <Caption><CaptionTitle>Tokens per day</CaptionTitle></Caption>
        <Empty>No requests in this period yet. Start a conversation to see usage here.</Empty>
      </Figure>
    );
  }

  const bandWidth: number = PLOT_WIDTH / daily.length;
  const barWidth: number = Math.min(34, bandWidth * 0.6);
  const gridValues: number[] = [0, 0.5, 1].map((fraction: number) => Math.round(scale * fraction));
  const hovered: UsageDailyPoint | null = hover ? daily[hover.index] : null;

  return (
    <Figure>
      <Caption>
        <CaptionTitle>Tokens per day</CaptionTitle>
        <Legend>
          <LegendItem $series="input">Input</LegendItem>
          <LegendItem $series="output">Output</LegendItem>
        </Legend>
      </Caption>

      <Plot>
        <Svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img"
          aria-label="Daily input and output token usage">
          {gridValues.map((value: number) => {
            const y: number = PADDING.top + PLOT_HEIGHT - (value / scale) * PLOT_HEIGHT;
            return (
              <g key={value}>
                <line x1={PADDING.left} y1={y} x2={WIDTH - PADDING.right} y2={y}
                  stroke="currentColor" strokeOpacity={0.1} />
                <AxisLabel x={PADDING.left - 8} y={y + 3} textAnchor="end">{compact(value)}</AxisLabel>
              </g>
            );
          })}

          {daily.map((point: UsageDailyPoint, index: number) => {
            const total: number = point.inputTokens + point.outputTokens;
            const x: number = PADDING.left + index * bandWidth + (bandWidth - barWidth) / 2;
            const inputHeight: number = (point.inputTokens / scale) * PLOT_HEIGHT;
            const outputHeight: number = (point.outputTokens / scale) * PLOT_HEIGHT;
            const baseline: number = PADDING.top + PLOT_HEIGHT;
            const showLabel: boolean = daily.length <= 14 || index % 2 === 0;

            const onEnter = (): void => setHover({ index, x: x + barWidth / 2, y: baseline - inputHeight - outputHeight });

            return (
              <g key={point.date} onMouseEnter={onEnter} onMouseLeave={(): void => setHover(null)}>
                {total === 0 ? (
                  <Bar x={x} y={baseline - 2} width={barWidth} height={2} rx={1}
                    fill="currentColor" fillOpacity={0.18} />
                ) : (
                  <>
                    <Bar x={x} y={baseline - inputHeight} width={barWidth} height={Math.max(inputHeight, 0)}
                      rx={2} fill="var(--usage-input)" />
                    {outputHeight > 0 && (
                      <Bar x={x} y={baseline - inputHeight - outputHeight - SEGMENT_GAP} width={barWidth}
                        height={outputHeight} rx={2} fill="var(--usage-output)" />
                    )}
                  </>
                )}
                {showLabel && (
                  <AxisLabel x={x + barWidth / 2} y={HEIGHT - 8} textAnchor="middle">
                    {shortDate(point.date)}
                  </AxisLabel>
                )}
              </g>
            );
          })}
        </Svg>

        {hover && hovered && (
          <Tooltip
            style={{ left: `${(hover.x / WIDTH) * 100}%`, top: `${(hover.y / HEIGHT) * 100}%` }}>
            <TooltipDate>{shortDate(hovered.date)}</TooltipDate>
            <TooltipRow $series="input"><span>{full(hovered.inputTokens)} in</span></TooltipRow>
            <TooltipRow $series="output"><span>{full(hovered.outputTokens)} out</span></TooltipRow>
            <TooltipRow><span>Total</span><span>{full(hovered.inputTokens + hovered.outputTokens)}</span></TooltipRow>
          </Tooltip>
        )}
      </Plot>
    </Figure>
  );
}
