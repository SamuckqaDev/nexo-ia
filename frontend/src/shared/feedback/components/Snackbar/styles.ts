import styled, { type DefaultTheme } from "styled-components";
import type { SnackbarVariant } from "../../types/snackbarTypes";

const variantColor = (variant: SnackbarVariant, theme: DefaultTheme): string => {
  if (variant === "success") return theme.colors.statusOnline;
  if (variant === "error") return theme.colors.danger;
  if (variant === "warning") return theme.colors.accent;
  return theme.colors.primary;
};

export const Region = styled.div`
  position: fixed;
  z-index: 1000;
  right: ${({ theme }) => theme.spacing.lg};
  bottom: ${({ theme }) => theme.spacing.lg};
  width: min(26rem, calc(100vw - 2rem));
`;

export const Message = styled.div<{ $variant: SnackbarVariant }>`
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme, $variant }) => variantColor($variant, theme)};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};
  color: ${({ theme, $variant }) => variantColor($variant, theme)};
  backdrop-filter: blur(18px);
  animation: enter 0.22s ease-out;

  @keyframes enter {
    from { opacity: 0; transform: translateY(0.75rem); }
    to { opacity: 1; transform: translateY(0); }
  }
`;

export const Text = styled.span`
  color: ${({ theme }) => theme.colors.text};
  font-size: 0.9rem;
  line-height: 1.5;
`;

export const Close = styled.button`
  display: grid;
  place-items: center;
  border: 0;
  padding: 0.25rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: ${({ theme }) => theme.colors.text};
  }
`;
