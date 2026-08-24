import styled, { css } from "styled-components";

export const Button = styled.button<{ $variant: "primary" | "outline" }>`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.primary};
  border-radius: 0.8rem;
  padding: 0.9rem 1rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;

  ${({ theme, $variant }) => $variant === "primary" ? css`
    background: ${theme.colors.primary};
    color: ${theme.colors.background};
  ` : css`
    background: transparent;
    color: ${theme.colors.primarySoft};
  `}

  &:hover:not(:disabled) {
    background: ${({ theme }) => theme.colors.primarySoft};
    color: ${({ theme }) => theme.colors.background};
    transform: translateY(-1px);
  }

  &:focus-visible {
    outline: 3px solid ${({ theme }) => theme.colors.lineStrong};
    outline-offset: 2px;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  &[aria-busy="true"]:disabled {
    cursor: progress;
  }
`;
