import styled from "styled-components";

export const Composer = styled.form`
  display: flex;
  gap: ${({ theme }) => theme.spacing.sm};
  align-items: flex-end;
  padding: ${({ theme }) => theme.spacing.md};
  border-top: 1px solid ${({ theme }) => theme.colors.line};
`;

export const Field = styled.textarea`
  flex: 1;
  min-height: 2.9rem;
  max-height: 12rem;
  resize: vertical;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => theme.spacing.sm};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.text};
  font: inherit;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 1px;
  }

  &:disabled {
    opacity: 0.6;
  }
`;

export const Hint = styled.p`
  margin: ${({ theme }) => `0 0 ${theme.spacing.xs}`};
  padding: ${({ theme }) => `0 ${theme.spacing.md}`};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.72rem;
`;
