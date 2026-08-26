import styled from "styled-components";

export const AdminGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.md};

  @media (max-width: 58rem) { grid-template-columns: 1fr; }
`;

export const Form = styled.form`
  display: grid;
  align-content: start;
  gap: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => theme.spacing.md};
  background: ${({ theme }) => theme.colors.background};

  > label { color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.64rem; font-weight: 700; }
  > small { color: ${({ theme }) => theme.colors.danger}; font-size: 0.58rem; }
`;

export const AdminCopy = styled.header`
  margin-bottom: 0.2rem;
  h4 { margin: 0; color: ${({ theme }) => theme.colors.text}; font-size: 0.76rem; }
  p { margin: 0.22rem 0 0; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.6rem; line-height: 1.5; }
`;

export const Textarea = styled.textarea`
  min-height: 6.4rem;
  resize: vertical;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.7rem;
  outline: 0;
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  font-size: 0.68rem;
  &:focus { border-color: ${({ theme }) => theme.colors.primary}; }
`;

export const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
  padding-top: 0.2rem;
`;
