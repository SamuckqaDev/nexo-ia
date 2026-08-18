import styled from "styled-components";

export const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
  gap: ${({ theme }) => theme.spacing.md};
`;

export const Panel = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  align-content: start;
`;

export const PanelTitle = styled.h4`
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: ${({ theme }) => theme.colors.textMuted};
`;

export const Row = styled.div`
  display: grid;
  gap: 0.4rem;
`;

export const RowHead = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const Name = styled.span`
  overflow: hidden;
  font-size: 0.82rem;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Detail = styled.span`
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.72rem;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
`;

export const Track = styled.div`
  height: 0.4rem;
  border-radius: 999px;
  background: ${({ theme }) => theme.colors.surfaceAccent};
  overflow: hidden;
`;

export const Fill = styled.div<{ $ratio: number; $tone: "primary" | "accent" }>`
  width: ${({ $ratio }) => `${Math.max($ratio * 100, 2)}%`};
  height: 100%;
  border-radius: 999px;
  background: ${({ theme, $tone }) =>
    $tone === "primary" ? theme.colors.primary : theme.colors.accent};
`;

export const Empty = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.76rem;
`;
