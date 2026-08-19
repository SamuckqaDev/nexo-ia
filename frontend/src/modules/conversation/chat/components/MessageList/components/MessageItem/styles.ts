import styled from "styled-components";

/** One full-width turn in the readable conversation column. */
export const Row = styled.article<{ $user: boolean }>`
  position: relative;
  display: grid;
  grid-template-columns: 2rem minmax(0, 1fr);
  gap: ${({ theme }) => theme.spacing.sm};
  width: min(60rem, 100%);
  align-self: center;
  padding: ${({ theme }) => `${theme.spacing.md} ${theme.spacing.md} ${theme.spacing.sm}`};
  border: 1px solid ${({ theme, $user }) => ($user ? theme.colors.line : theme.colors.lineStrong)};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme, $user }) => ($user ? theme.colors.surface : theme.colors.surfaceStrong)};

  /* A cyan rail marks a Nexo answer as the primary content; the person's turn stays quieter. */
  &::before {
    content: "";
    position: absolute;
    top: ${({ theme }) => theme.spacing.md};
    bottom: ${({ theme }) => theme.spacing.md};
    left: -1px;
    width: 2px;
    border-radius: ${({ theme }) => theme.radius.round};
    background: ${({ theme, $user }) => ($user ? "transparent" : theme.colors.primary)};
  }
`;

export const Avatar = styled.span<{ $user: boolean }>`
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme, $user }) => ($user ? theme.colors.dangerSurface : theme.colors.surfaceAccent)};
  color: ${({ theme, $user }) => ($user ? theme.colors.accentSoft : theme.colors.primary)};
  line-height: 0;

  svg { display: block; }
`;

export const Column = styled.div`
  min-width: 0;
`;

export const Head = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  min-height: 1.4rem;
  margin-bottom: 0.25rem;
`;

export const Name = styled.span<{ $user: boolean }>`
  color: ${({ theme, $user }) => ($user ? theme.colors.textMuted : theme.colors.primarySoft)};
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
`;

/**
 * The copy control stays out of the way until the row is hovered or the button itself is focused,
 * so a screen-reader or keyboard user can still reach it.
 */
export const CopyButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.24rem 0.4rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  font: inherit;
  font-size: 0.64rem;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s ease, color 0.15s ease;

  ${Row}:hover &,
  &:focus-visible {
    opacity: 1;
  }

  &:hover:not(:disabled),
  &:focus-visible {
    background: ${({ theme }) => theme.colors.surfaceAccent};
    color: ${({ theme }) => theme.colors.primary};
  }

  &:disabled { cursor: not-allowed; opacity: 0; }
`;

export const Body = styled.div<{ $user: boolean }>`
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: ${({ theme }) => theme.colors.text};
  font-size: ${({ $user }) => ($user ? "0.85rem" : "0.9rem")};
  line-height: 1.72;
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
  gap: 0.55rem;
  margin: ${({ theme }) => `${theme.spacing.sm} 0 0`};
  padding-top: ${({ theme }) => theme.spacing.xs};
  border-top: 1px solid ${({ theme }) => theme.colors.line};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.64rem;
  font-variant-numeric: tabular-nums;
`;

export const Caret = styled.span`
  display: inline-block;
  width: 0.5rem;
  color: ${({ theme }) => theme.colors.primary};
  animation: blink 1s steps(2, start) infinite;

  @keyframes blink { to { visibility: hidden; } }
  @media (prefers-reduced-motion: reduce) { animation: none; }
`;
