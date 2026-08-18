import styled from "styled-components";

export const Bubble = styled.article<{ $user: boolean }>`
  max-width: min(48rem, 85%);
  align-self: ${({ $user }) => ($user ? "flex-end" : "flex-start")};
  border: 1px solid ${({ theme, $user }) => ($user ? "transparent" : theme.colors.line)};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => `${theme.spacing.sm} ${theme.spacing.md}`};
  background: ${({ theme, $user }) => ($user ? theme.colors.primary : theme.colors.surfaceAccent)};
  color: ${({ theme, $user }) => ($user ? theme.colors.background : theme.colors.text)};
  white-space: pre-wrap;
  overflow-wrap: anywhere;
`;

export const Badge = styled.p<{ $tone: "warning" | "danger" }>`
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin: ${({ theme }) => `${theme.spacing.xs} 0 0`};
  color: ${({ theme, $tone }) => ($tone === "danger" ? theme.colors.danger : theme.colors.accentSoft)};
  font-size: 0.72rem;
  font-weight: 700;
`;

export const Meta = styled.p`
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin: ${({ theme }) => `${theme.spacing.xs} 0 0`};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.68rem;
  font-variant-numeric: tabular-nums;
`;

export const Caret = styled.span`
  display: inline-block;
  width: 0.5rem;
  color: ${({ theme }) => theme.colors.primary};
  animation: blink 1s steps(2, start) infinite;

  @keyframes blink {
    to {
      visibility: hidden;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;
