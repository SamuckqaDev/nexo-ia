import styled from "styled-components";

export const Card = styled.section`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};

  @media (max-width: 48rem) {
    grid-template-columns: auto minmax(0, 1fr);
  }
`;

export const Copy = styled.div`
  display: grid;
  min-width: 0;
  gap: 0.25rem;
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.78rem; }
  > span { color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.64rem; line-height: 1.5; }
`;

export const RuntimeState = styled.span<{ $connected: boolean }>`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  width: fit-content;
  color: ${({ theme, $connected }) => $connected ? theme.colors.statusOnline : theme.colors.textSubtle};
  font-size: 0.6rem;
  font-weight: 700;
`;

export const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacing.sm};
  @media (max-width: 48rem) { grid-column: 1 / -1; }
`;

export const ErrorCopy = styled.p`
  grid-column: 1 / -1;
  margin: 0;
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.62rem;
`;
