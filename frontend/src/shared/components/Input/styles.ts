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

export const Control = styled.div<{ $invalid: boolean }>`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  min-width: 0;
  min-height: 3.1rem;
  padding: 0 ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme, $invalid }) => $invalid ? theme.colors.danger : theme.colors.lineStrong};
  border-radius: 0.8rem;
  background: ${({ theme }) => theme.colors.background};
  color: ${({ theme, $invalid }) => $invalid ? theme.colors.danger : theme.colors.primary};
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within {
    border-color: ${({ theme, $invalid }) => $invalid ? theme.colors.danger : theme.colors.primary};
    box-shadow: 0 0 0 3px ${({ theme }) => theme.colors.line};
  }

  &:has(input:disabled) {
    cursor: not-allowed;
    opacity: 0.55;
  }
`;

export const NativeInput = styled.input`
  min-width: 0;
  flex: 1;
  border: 0;
  padding: 0.85rem 0;
  background: transparent;
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  outline: none;

  &::placeholder {
    color: ${({ theme }) => theme.colors.textSubtle};
  }
`;

export const IconButton = styled.button`
  display: grid;
  place-items: center;
  border: 0;
  padding: 0.25rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;

  &:hover:not(:disabled),
  &:focus-visible {
    color: ${({ theme }) => theme.colors.primary};
  }
`;

export const ActionButton = styled.button`
  flex: 0 0 auto;
  border: 0;
  border-left: 1px solid ${({ theme }) => theme.colors.line};
  padding: 0.35rem 0 0.35rem ${({ theme }) => theme.spacing.sm};
  background: transparent;
  color: ${({ theme }) => theme.colors.primarySoft};
  font: inherit;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;

  &:hover:not(:disabled),
  &:focus-visible {
    color: ${({ theme }) => theme.colors.primary};
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
