import styled from "styled-components";

export const Form = styled.form`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
`;

export const Picker = styled.div`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceAccent};

  > span {
    display: grid;
    width: 3rem;
    height: 3rem;
    place-items: center;
    border-radius: ${({ theme }) => theme.radius.control};
    background: ${({ theme }) => theme.colors.background};
    color: ${({ theme }) => theme.colors.primary};
  }

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.8rem; }
  p { margin: 0.25rem 0 0; color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.64rem; line-height: 1.55; }
  > button { grid-column: 1 / -1; width: 100%; }
`;

export const Notice = styled.p`
  margin: 0;
  padding: ${({ theme }) => theme.spacing.sm};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.65rem;
  line-height: 1.55;
`;

export const ErrorMessage = styled.p`
  margin: 0;
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.accent};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.65rem;
  line-height: 1.55;
`;

export const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacing.sm};
`;
