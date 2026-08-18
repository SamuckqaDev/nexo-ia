import styled from "styled-components";

export const Figure = styled.figure`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  margin: 0;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
`;

export const Caption = styled.figcaption`
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const CaptionTitle = styled.span`
  font-size: 0.85rem;
  font-weight: 700;
`;

export const Legend = styled.div`
  display: flex;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const LegendItem = styled.span<{ $series: "input" | "output" }>`
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.72rem;

  &::before {
    content: "";
    width: 0.7rem;
    height: 0.7rem;
    border-radius: 3px;
    background: ${({ theme, $series }) =>
      $series === "input" ? theme.colors.primary : theme.colors.accent};
  }
`;

export const Plot = styled.div`
  position: relative;
  overflow-x: auto;
`;

export const Svg = styled.svg`
  display: block;
  width: 100%;
  height: auto;
  overflow: visible;
`;

export const Bar = styled.rect`
  transition: opacity 0.15s ease;
  cursor: pointer;

  &:hover {
    opacity: 0.82;
  }
`;

export const AxisLabel = styled.text`
  fill: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  font-variant-numeric: tabular-nums;
`;

export const Tooltip = styled.div`
  position: absolute;
  transform: translate(-50%, calc(-100% - 0.6rem));
  min-width: 9rem;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => `${theme.spacing.xs} ${theme.spacing.sm}`};
  background: ${({ theme }) => theme.colors.backgroundElevated};
  box-shadow: ${({ theme }) => theme.shadow};
  pointer-events: none;
  z-index: 2;
`;

export const TooltipDate = styled.p`
  margin: 0 0 0.3rem;
  font-size: 0.72rem;
  font-weight: 700;
`;

export const TooltipRow = styled.p<{ $series?: "input" | "output" }>`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  margin: 0.12rem 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.7rem;
  font-variant-numeric: tabular-nums;

  &::before {
    content: "";
    display: ${({ $series }) => ($series ? "inline-block" : "none")};
    width: 0.55rem;
    height: 0.55rem;
    margin-right: auto;
    border-radius: 2px;
    background: ${({ theme, $series }) =>
      $series === "input" ? theme.colors.primary : theme.colors.accent};
  }
`;

export const Empty = styled.p`
  margin: 0;
  padding: ${({ theme }) => theme.spacing.xl};
  color: ${({ theme }) => theme.colors.textSubtle};
  text-align: center;
`;
