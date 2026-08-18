import styled from "styled-components";

export const Overview = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.lg};

  /* Series colors for the SVG chart, taken from the brand tokens so both themes stay consistent. */
  --usage-input: ${({ theme }) => theme.colors.primary};
  --usage-output: ${({ theme }) => theme.colors.accent};
`;

export const Toolbar = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const Periods = styled.div`
  display: inline-flex;
  gap: 0.25rem;
  padding: 0.25rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceStrong};
`;

export const PeriodButton = styled.button<{ $active: boolean }>`
  border: 0;
  border-radius: calc(${({ theme }) => theme.radius.control} - 0.25rem);
  padding: 0.42rem 0.75rem;
  background: ${({ theme, $active }) => ($active ? theme.colors.primary : "transparent")};
  color: ${({ theme, $active }) => ($active ? theme.colors.background : theme.colors.textMuted)};
  font: inherit;
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 1px;
  }
`;

export const Scope = styled.span`
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.72rem;
`;

export const Failure = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  justify-items: start;
  border: 1px solid ${({ theme }) => theme.colors.danger};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.lg};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.danger};
`;
