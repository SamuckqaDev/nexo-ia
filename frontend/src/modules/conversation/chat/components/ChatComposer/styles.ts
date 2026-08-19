import styled from "styled-components";

export const ComposerCard = styled.div`
  width: min(60rem, calc(100% - 2rem));
  margin: 0 auto ${({ theme }) => theme.spacing.lg};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.16);
  overflow: hidden;
`;

export const Composer = styled.form`
  display: grid;
`;

export const Field = styled.textarea`
  flex: 1;
  min-height: 3rem;
  max-height: 12rem;
  resize: none;
  border: 0;
  padding: ${({ theme }) => `${theme.spacing.sm} ${theme.spacing.md}`};
  background: transparent;
  line-height: 1.5;
  color: ${({ theme }) => theme.colors.text};
  font: inherit;

  &:focus-visible {
    outline: none;
  }

  &:disabled {
    opacity: 0.6;
  }
`;

export const Hint = styled.p`
  margin: 0;
  padding: ${({ theme }) => `${theme.spacing.sm} ${theme.spacing.md} 0`};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.72rem;
`;

export const ModeHint = styled.p`
  margin: 0;
  padding: ${({ theme }) => `${theme.spacing.sm} ${theme.spacing.md}`};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.72rem;
  line-height: 1.5;
`;

export const ComposerFooter = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
  padding: ${({ theme }) => theme.spacing.sm};
  border-top: 1px solid ${({ theme }) => theme.colors.line};
`;

export const ModeControl = styled.div`
  display: flex;
  gap: 0.2rem;
  padding: 0.2rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.backgroundSoft};
`;

export const ModeButton = styled.button<{ $active: boolean; $agent?: boolean }>`
  display: flex;
  align-items: center;
  gap: 0.35rem;
  border: 0;
  border-radius: 0.6rem;
  padding: 0.42rem 0.62rem;
  background: ${({ theme, $active, $agent }) => $active
    ? $agent ? theme.colors.dangerSurface : theme.colors.surfaceAccent
    : "transparent"};
  color: ${({ theme, $active, $agent }) => $active
    ? $agent ? theme.colors.accentSoft : theme.colors.primarySoft
    : theme.colors.textMuted};
  font: inherit;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
`;

export const CapabilityButton = styled.button`
  display: flex;
  align-items: center;
  gap: 0.3rem;
  border: 0;
  padding: 0.4rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  font: inherit;
  font-size: 0.7rem;
  &:disabled { cursor: not-allowed; opacity: 0.65; }
  @media (max-width: 38rem) { span { display: none; } }
`;

export const SendButton = styled.button`
  display: grid;
  width: 2.5rem;
  height: 2.5rem;
  margin-left: auto;
  place-items: center;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.background};
  cursor: pointer;
  &:hover:not(:disabled) { background: ${({ theme }) => theme.colors.primarySoft}; }
  &:disabled { cursor: not-allowed; opacity: 0.38; }
`;
