import styled from "styled-components";

export const TreeFrame = styled.div<{ $compact: boolean }>`
  display: grid;
  min-height: 0;
  max-height: ${({ $compact }) => $compact ? "22rem" : "28rem"};
  overflow: auto;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
`;

export const Meta = styled.div`
  position: sticky;
  z-index: 1;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.sm};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.62rem;
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.55rem; }
`;

export const Tree = styled.ul`
  display: grid;
  padding: 0;
  margin: 0;
  list-style: none;
`;

export const Node = styled.li`
  min-width: 0;
`;

export const NodeButton = styled.button<{ $depth: number }>`
  display: grid;
  grid-template-columns: 0.8rem 1rem minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.38rem;
  width: 100%;
  min-height: 2rem;
  border: 0;
  padding: 0.32rem 0.55rem 0.32rem calc(0.55rem + ${({ $depth }) => $depth} * 0.8rem);
  background: transparent;
  color: ${({ theme }) => theme.colors.primary};
  font: inherit;
  text-align: left;
  cursor: pointer;
  > svg:first-child { transition: transform 0.15s ease; }
  > svg:first-child.open { transform: rotate(90deg); }
  > i { width: 0.8rem; }
  > span { overflow: hidden; color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.65rem; text-overflow: ellipsis; white-space: nowrap; }
  > small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.54rem; }
  &:hover, &:focus-visible { background: ${({ theme }) => theme.colors.surfaceAccent}; }
`;
