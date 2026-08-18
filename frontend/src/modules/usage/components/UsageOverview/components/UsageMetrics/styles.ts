import styled from "styled-components";

export const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: ${({ theme }) => theme.spacing.md};
`;

export const Tile = styled.div`
  display: grid;
  gap: 0.35rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
`;

export const Label = styled.span`
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.02em;
`;

export const Value = styled.strong`
  font-size: 1.5rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
`;

export const Note = styled.span`
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.7rem;
`;

export const StatusRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 0.15rem;
`;

export const StatusPill = styled.span<{ $tone: "good" | "warning" | "danger" }>`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: ${({ theme, $tone }) =>
    $tone === "good"
      ? theme.colors.statusOnline
      : $tone === "danger"
        ? theme.colors.danger
        : theme.colors.accentSoft};

  &::before {
    content: "";
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 999px;
    background: currentColor;
  }
`;
