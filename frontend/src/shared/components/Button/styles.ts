import styled, { css } from "styled-components";

export const Button = styled.button<{
  $variant: "primary" | "outline";
  $size: "default" | "compact";
}>`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: ${({ theme, $size }) => $size === "compact" ? theme.spacing.xs : theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.primary};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ $size }) => $size === "compact" ? "0.46rem 0.66rem" : "0.65rem 0.82rem"};
  font: inherit;
  font-size: ${({ $size }) => $size === "compact" ? "0.7rem" : "0.78rem"};
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
