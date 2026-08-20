import styled from "styled-components";

export const Notice = styled.section<{ $tone: "info" | "warning" }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => `${theme.spacing.sm} ${theme.spacing.md}`};
  border-bottom: 1px solid ${({ theme, $tone }) => $tone === "warning" ? theme.colors.accent : theme.colors.lineStrong};
  background: ${({ theme, $tone }) => $tone === "warning" ? theme.colors.dangerSurface : theme.colors.surfaceAccent};
  color: ${({ theme, $tone }) => $tone === "warning" ? theme.colors.accentSoft : theme.colors.primary};

  @media (max-width: 52rem) {
    grid-template-columns: auto minmax(0, 1fr);
  }
`;

export const Copy = styled.div`
  display: grid;
  min-width: 0;
  gap: 0.15rem;

  strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 0.75rem;
  }

  span {
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.64rem;
    line-height: 1.45;
  }
`;

export const Summary = styled.span`
  color: ${({ theme }) => theme.colors.accentSoft} !important;
  font-weight: 700;
`;

export const Samples = styled.code`
  overflow: hidden;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.57rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacing.xs};

  button {
    padding: 0.55rem 0.7rem;
    font-size: 0.64rem;
  }

  @media (max-width: 52rem) {
    grid-column: 1 / -1;
    justify-content: stretch;

    button { flex: 1; }
  }
`;
