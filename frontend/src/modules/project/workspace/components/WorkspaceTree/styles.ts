import styled from "styled-components";

export const TreeFrame = styled.div<{ $compact: boolean }>`
  display: grid;
  min-height: 0;
  max-height: ${({ $compact }) => $compact ? "22rem" : "min(38rem, 68dvh)"};
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
`;

export const MetaCopy = styled.span`
  display: grid;
  min-width: 0;
  gap: 0.08rem;
  font-size: 0.62rem;

  > small {
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.55rem;
  }
`;

export const TreeTools = styled.span`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const ToolButton = styled.button`
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.round};
  padding: 0.28rem 0.5rem;
  background: ${({ theme }) => theme.colors.background};
  color: ${({ theme }) => theme.colors.primarySoft};
  font: inherit;
  font-size: 0.56rem;
  font-weight: 700;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    border-color: ${({ theme }) => theme.colors.primary};
    color: ${({ theme }) => theme.colors.primary};
  }
`;

export const Search = styled.label`
  position: sticky;
  z-index: 1;
  top: 3rem;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
  margin: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.48rem 0.6rem;
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.textSubtle};

  &:focus-within {
    border-color: ${({ theme }) => theme.colors.primary};
  }

  > input {
    width: 100%;
    min-width: 0;
    border: 0;
    outline: 0;
    background: transparent;
    color: ${({ theme }) => theme.colors.text};
    font: inherit;
    font-size: 0.64rem;
  }
`;

export const ScanNotice = styled.details`
  margin: 0 ${({ theme }) => theme.spacing.sm} ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.accent};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => theme.spacing.sm};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.58rem;

  > summary {
    display: flex;
    align-items: center;
    gap: ${({ theme }) => theme.spacing.xs};
    font-weight: 700;
    cursor: pointer;
  }

  > p {
    margin: ${({ theme }) => theme.spacing.sm} 0;
    color: ${({ theme }) => theme.colors.textMuted};
    line-height: 1.5;
  }

  > small {
    color: ${({ theme }) => theme.colors.textSubtle};
  }
`;

export const OmissionList = styled.ul`
  display: grid;
  gap: 0.3rem;
  max-height: 10rem;
  overflow: auto;
  margin: 0 0 ${({ theme }) => theme.spacing.sm};
  padding: 0;
  list-style: none;

  > li {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: ${({ theme }) => theme.spacing.sm};
  }

  code {
    overflow: hidden;
    color: ${({ theme }) => theme.colors.text};
    font-size: 0.56rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: ${({ theme }) => theme.colors.textSubtle};
  }
`;

export const EmptyFilter = styled.p`
  margin: 0;
  padding: ${({ theme }) => theme.spacing.lg};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.64rem;
  text-align: center;
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
