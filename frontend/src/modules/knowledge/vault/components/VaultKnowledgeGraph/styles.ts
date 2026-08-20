import styled, { css } from "styled-components";

export const GraphShell = styled.div`
  display: grid;
  min-width: 0;
  background:
    radial-gradient(circle at 50% 42%, ${({ theme }) => theme.colors.surfaceAccent}, transparent 28rem),
    ${({ theme }) => theme.colors.background};
`;

export const GraphToolbar = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: 0.7rem ${({ theme }) => theme.spacing.md};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
`;

export const Legend = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const LegendItem = styled.span<{ $kind: "vault" | "source" | "attached" | "related" }>`
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.58rem;

  &::before {
    width: 0.55rem;
    height: 0.55rem;
    border: 1px solid ${({ theme }) => theme.colors.lineStrong};
    border-radius: 50%;
    background: ${({ theme, $kind }) => $kind === "vault" ? theme.colors.primary : theme.colors.surfaceStrong};
    content: "";
  }

  ${({ theme, $kind }) => $kind === "attached" && css`
    &::before { border-color: ${theme.colors.statusOnline}; background: ${theme.colors.statusOnline}; box-shadow: 0 0 0.7rem ${theme.colors.statusOnlineGlow}; }
  `}

  ${({ theme, $kind }) => $kind === "related" && css`
    &::before { width: 1rem; height: 0; border: 0; border-top: 1px dashed ${theme.colors.accentSoft}; border-radius: 0; background: transparent; }
  `}
`;

export const GraphSummary = styled.span`
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.6rem;
`;

export const GraphViewport = styled.div`
  max-height: min(31rem, 54vh);
  min-height: 22rem;
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
`;

export const GraphCanvas = styled.div<{ $width: number; $height: number }>`
  position: relative;
  width: ${({ $width }) => $width}px;
  height: ${({ $height }) => $height}px;
  margin-inline: auto;
`;

export const EdgeLayer = styled.svg`
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
`;

export const Edge = styled.line<{ $related: boolean }>`
  stroke: ${({ theme, $related }) => $related ? theme.colors.accentSoft : theme.colors.lineStrong};
  stroke-width: ${({ $related }) => $related ? 1.25 : 1};
  stroke-dasharray: ${({ $related }) => $related ? "6 7" : "none"};
  opacity: ${({ $related }) => $related ? 0.58 : 0.72};
`;

export const GraphNode = styled.button<{
  $kind: "vault" | "source";
  $x: number;
  $y: number;
  $attached: boolean;
  $selected: boolean;
}>`
  position: absolute;
  left: ${({ $x }) => $x}px;
  top: ${({ $y }) => $y}px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  width: ${({ $kind }) => $kind === "vault" ? "11.5rem" : "9.6rem"};
  align-items: center;
  gap: 0.55rem;
  transform: translate(-50%, -50%);
  border: 1px solid ${({ theme, $attached, $selected }) =>
    $attached ? theme.colors.statusOnline : $selected ? theme.colors.primary : theme.colors.lineStrong};
  border-radius: ${({ theme, $kind }) => $kind === "vault" ? theme.radius.round : theme.radius.control};
  padding: ${({ $kind }) => $kind === "vault" ? ".62rem .8rem" : ".55rem .65rem"};
  background: ${({ theme, $kind }) => $kind === "vault" ? theme.colors.backgroundElevated : theme.colors.surfaceStrong};
  box-shadow: ${({ theme, $attached, $selected }) =>
    $attached ? `0 0 1.25rem ${theme.colors.statusOnlineGlow}` : $selected ? theme.shadow : "none"};
  color: ${({ theme, $kind }) => $kind === "vault" ? theme.colors.primarySoft : theme.colors.textMuted};
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;

  &:hover, &:focus-visible {
    z-index: 2;
    transform: translate(-50%, -50%) scale(1.035);
    border-color: ${({ theme }) => theme.colors.primary};
    outline: none;
  }

  > span { min-width: 0; }
  strong, small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.66rem; }
  small { margin-top: 0.12rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.52rem; }

  @media (max-width: 38rem) {
    width: ${({ $kind }) => $kind === "vault" ? "10.4rem" : "8.8rem"};
  }
`;

export const EmptyGraph = styled.div`
  display: grid;
  min-height: 22rem;
  place-items: center;
  padding: ${({ theme }) => theme.spacing.xl};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.72rem;
  text-align: center;
`;
