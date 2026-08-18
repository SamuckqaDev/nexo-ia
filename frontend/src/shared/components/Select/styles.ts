import styled from "styled-components";

export const Field = styled.div`
  display: grid;
  gap: 0.4rem;
  width: 100%;
  min-width: 0;
`;

export const Label = styled.label`
  font-size: 0.84rem;
  font-weight: 600;
`;

export const Control = styled.select<{ $invalid: boolean }>`
  width: 100%;
  min-height: 3.1rem;
  border: 1px solid ${({ theme, $invalid }) => $invalid ? theme.colors.danger : theme.colors.lineStrong};
  border-radius: 0.8rem;
  padding: 0 2.5rem 0 ${({ theme }) => theme.spacing.md};
  appearance: auto;
  background: ${({ theme }) => theme.colors.background};
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-visible {
    border-color: ${({ theme, $invalid }) => $invalid ? theme.colors.danger : theme.colors.primary};
    box-shadow: 0 0 0 3px ${({ theme }) => theme.colors.line};
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
`;

export const Error = styled.span`
  color: ${({ theme }) => theme.colors.danger};
  font-size: 0.78rem;
`;

export const Helper = styled.span`
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.76rem;
  line-height: 1.45;
`;
