import styled, { css } from "styled-components";
import type { KnowledgeGraphNodeKind } from "../../types/vaultGraphTypes";

export const GraphShell = styled.div`
  display: grid;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr) auto;
  background:
    radial-gradient(circle at 50% 46%, ${({ theme }) => theme.colors.surfaceAccent}, transparent 30rem),
    ${({ theme }) => theme.colors.background};
`;

export const GraphToolbar = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: 0.58rem ${({ theme }) => theme.spacing.md};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.surface};

  @media (max-width: 48rem) { align-items: stretch; }
`;

export const GraphSearch = styled.label`
  display: flex;
  width: min(15rem, 100%);
  align-items: center;
  gap: 0.4rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.42rem 0.58rem;
  background: ${({ theme }) => theme.colors.background};
  color: ${({ theme }) => theme.colors.textSubtle};

  &:focus-within { border-color: ${({ theme }) => theme.colors.primary}; }

  input {
    min-width: 0;
    flex: 1;
    border: 0;
    outline: 0;
    background: transparent;
    color: ${({ theme }) => theme.colors.text};
    font: inherit;
    font-size: 0.64rem;
  }

  @media (max-width: 48rem) { width: 100%; }
`;

export const Legend = styled.div`
  display: flex;
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing.sm};

  @media (max-width: 48rem) { order: 3; flex-basis: 100%; }
`;

export const LegendItem = styled.span<{ $kind: "vault" | "source" | "chunk" | "semantic" }>`
  display: inline-flex;
  align-items: center;
  gap: 0.32rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.56rem;

  &::before {
    width: ${({ $kind }) => $kind === "vault" ? ".68rem" : $kind === "source" ? ".56rem" : ".4rem"};
    height: ${({ $kind }) => $kind === "semantic" ? "0" : $kind === "vault" ? ".68rem" : $kind === "source" ? ".56rem" : ".4rem"};
    border: 1px solid ${({ theme }) => theme.colors.lineStrong};
    border-radius: 50%;
    background: ${({ theme, $kind }) => $kind === "vault"
      ? theme.colors.primary
      : $kind === "source" ? theme.colors.primarySoft : theme.colors.surfaceStrong};
    content: "";
  }

  ${({ theme, $kind }) => $kind === "semantic" && css`
    &::before {
      width: 1rem;
      border: 0;
      border-top: 1px dashed ${theme.colors.accentSoft};
      border-radius: 0;
      background: transparent;
    }
  `}
`;

export const GraphActions = styled.div`
  display: flex;
  align-items: center;
  gap: 0.3rem;

  @media (max-width: 48rem) { margin-left: auto; }
`;

export const ToolButton = styled.button<{ $active?: boolean }>`
  display: inline-flex;
  min-width: 1.8rem;
  height: 1.8rem;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  border: 1px solid ${({ $active, theme }) => $active ? theme.colors.primary : theme.colors.line};
  border-radius: 0.5rem;
  padding: 0 0.45rem;
  background: ${({ $active, theme }) => $active ? theme.colors.surfaceAccent : theme.colors.background};
  color: ${({ $active, theme }) => $active ? theme.colors.primary : theme.colors.textSubtle};
  font: inherit;
  font-size: 0.56rem;
  cursor: pointer;

  &:hover, &:focus-visible {
    border-color: ${({ theme }) => theme.colors.primary};
    color: ${({ theme }) => theme.colors.primary};
    outline: none;
  }
`;

export const GraphSummary = styled.span`
  padding: 0.36rem ${({ theme }) => theme.spacing.md};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.56rem;
  text-align: right;
`;

export const GraphStage = styled.div`
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
`;

export const GraphViewport = styled.div`
  height: 100%;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  background-image: radial-gradient(circle, ${({ theme }) => theme.colors.lineStrong} 0.75px, transparent 0.9px);
  background-size: 1.4rem 1.4rem;
`;

export const GraphCanvas = styled.div<{ $width: number; $height: number }>`
  position: relative;
  width: ${({ $width }) => $width}px;
  height: ${({ $height }) => $height}px;
  min-width: 100%;
  min-height: 100%;
  margin-inline: auto;
`;

export const GraphScene = styled.div<{ $width: number; $height: number; $zoom: number }>`
  position: absolute;
  left: 50%;
  top: 50%;
  width: ${({ $width }) => $width}px;
  height: ${({ $height }) => $height}px;
  transform: translate(-50%, -50%) scale(${({ $zoom }) => $zoom});
  transform-origin: center;
`;

export const EdgeLayer = styled.svg`
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
`;

export const Edge = styled.line<{ $semantic: boolean; $strength: number }>`
  stroke: ${({ theme, $semantic }) => $semantic ? theme.colors.accentSoft : theme.colors.lineStrong};
  stroke-width: ${({ $semantic, $strength }) => $semantic ? 0.8 + $strength * 1.8 : 1};
  stroke-dasharray: ${({ $semantic }) => $semantic ? "5 6" : "none"};
  opacity: ${({ $semantic, $strength }) => $semantic ? 0.2 + $strength * 0.56 : 0.56};
`;

export const GraphNode = styled.button<{
  $kind: KnowledgeGraphNodeKind;
  $x: number;
  $y: number;
  $selected: boolean;
  $muted: boolean;
}>`
  position: absolute;
  z-index: ${({ $kind }) => $kind === "VAULT" ? 3 : $kind === "SOURCE" ? 2 : 1};
  left: ${({ $x }) => $x}px;
  top: ${({ $y }) => $y}px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  width: ${({ $kind }) => $kind === "VAULT" ? "11.5rem" : $kind === "SOURCE" ? "9.4rem" : "6.4rem"};
  align-items: center;
  gap: ${({ $kind }) => $kind === "CHUNK" ? ".34rem" : ".5rem"};
  transform: translate(-50%, -50%);
  border: 1px solid ${({ theme, $kind, $selected }) => $selected
    ? theme.colors.primary
    : $kind === "CHUNK" ? theme.colors.line : theme.colors.lineStrong};
  border-radius: ${({ theme, $kind }) => $kind === "VAULT" ? theme.radius.round : $kind === "CHUNK" ? ".65rem" : theme.radius.control};
  padding: ${({ $kind }) => $kind === "VAULT" ? ".6rem .75rem" : $kind === "SOURCE" ? ".52rem .62rem" : ".38rem .46rem"};
  background: ${({ theme, $kind }) => $kind === "VAULT"
    ? theme.colors.backgroundElevated
    : $kind === "SOURCE" ? theme.colors.surfaceStrong : theme.colors.background};
  box-shadow: ${({ theme, $selected }) => $selected ? `0 0 1.2rem ${theme.colors.statusOnlineGlow}` : "none"};
  color: ${({ theme, $kind }) => $kind === "VAULT" ? theme.colors.primary : $kind === "SOURCE" ? theme.colors.primarySoft : theme.colors.textSubtle};
  font: inherit;
  text-align: left;
  cursor: pointer;
  opacity: ${({ $muted }) => $muted ? 0.18 : 1};
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease, opacity 150ms ease;

  &:hover, &:focus-visible {
    z-index: 4;
    transform: translate(-50%, -50%) scale(1.045);
    border-color: ${({ theme }) => theme.colors.primary};
    outline: none;
  }

  > span { min-width: 0; }
  strong, small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: ${({ $kind }) => $kind === "CHUNK" ? ".56rem" : ".64rem"}; }
  small { margin-top: 0.1rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: ${({ $kind }) => $kind === "CHUNK" ? ".46rem" : ".5rem"}; }

  @media (prefers-reduced-motion: reduce) { transition: none; }
`;

export const Inspector = styled.aside`
  position: absolute;
  z-index: 8;
  right: 0.8rem;
  bottom: 0.8rem;
  display: grid;
  width: min(22rem, calc(100% - 1.6rem));
  gap: 0.7rem;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.8rem;
  background: color-mix(in srgb, ${({ theme }) => theme.colors.backgroundElevated} 94%, transparent);
  box-shadow: ${({ theme }) => theme.shadow};
  backdrop-filter: blur(12px);

  header, footer { display: flex; align-items: center; justify-content: space-between; gap: 0.6rem; }
  header > div { min-width: 0; }
  header strong, header span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  header strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.72rem; }
  header span, footer { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.52rem; }
`;

export const InspectorBadge = styled.span`
  flex: 0 0 auto;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.round};
  padding: 0.2rem 0.4rem;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 0.48rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
`;

export const InspectorCopy = styled.p`
  max-height: 8rem;
  overflow: auto;
  margin: 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.62rem;
  line-height: 1.6;
`;

export const EmptyGraph = styled.div`
  display: grid;
  height: 100%;
  min-height: 13rem;
  place-items: center;
  padding: ${({ theme }) => theme.spacing.xl};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.72rem;
  text-align: center;
`;
