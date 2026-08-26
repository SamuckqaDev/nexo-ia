import styled from "styled-components";

export const ComposerCard = styled.div`
  position: relative;
  z-index: 1;
  width: min(60rem, calc(100% - 2rem));
  margin: 0 auto ${({ theme }) => theme.spacing.lg};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.16);
  overflow: visible;
`;

export const ComposerSurface = styled.div`
  overflow: hidden;
  border-radius: inherit;
`;

export const Composer = styled.form`
  position: relative;
  display: grid;
`;

export const ActiveContext = styled.div`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => `${theme.spacing.xs} ${theme.spacing.md}`};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;

export const ActiveContextCopy = styled.span`
  display: grid;
  min-width: 0;
  gap: 0.12rem;
  strong { color: ${({ theme }) => theme.colors.primarySoft}; font-size: 0.72rem; }
  span { overflow: hidden; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.6rem; text-overflow: ellipsis; white-space: nowrap; }
`;

export const RemoveContext = styled.button`
  display: grid;
  width: 1.75rem;
  height: 1.75rem;
  place-items: center;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.button};
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  cursor: pointer;
  &:hover, &:focus-visible { background: ${({ theme }) => theme.colors.surfaceStrong}; color: ${({ theme }) => theme.colors.primary}; }
`;

export const SkillMenu = styled.div`
  position: absolute;
  z-index: 8;
  right: ${({ theme }) => theme.spacing.sm};
  bottom: calc(100% - 0.35rem);
  left: ${({ theme }) => theme.spacing.sm};
  display: grid;
  max-height: min(25rem, 55vh);
  overflow-y: auto;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: 0.35rem;
  background: ${({ theme }) => theme.colors.backgroundElevated};
  box-shadow: ${({ theme }) => theme.shadow};
`;

export const SkillPaletteHint = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.sm};
  color: ${({ theme }) => theme.colors.textSubtle};
  span { color: ${({ theme }) => theme.colors.text}; font-size: 0.7rem; font-weight: 700; }
  small { font-size: 0.58rem; }
`;

export const SkillOption = styled.button<{ $active: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 1px solid ${({ theme, $active }) => $active ? theme.colors.lineStrong : "transparent"};
  border-radius: ${({ theme }) => theme.radius.button};
  padding: ${({ theme }) => theme.spacing.sm};
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ theme }) => theme.colors.primary};
  font: inherit;
  text-align: left;
  cursor: pointer;
`;

export const SkillCopy = styled.span`
  display: grid;
  min-width: 0;
  gap: 0.16rem;
  strong, span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.7rem; }
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.6rem; }
`;

export const SkillCommand = styled.code`
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.62rem;
`;

export const HistoryMenu = styled(SkillMenu)`
  max-height: min(20rem, 48vh);
`;

export const HistoryOption = styled.button<{ $active: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 1px solid ${({ theme, $active }) => $active ? theme.colors.lineStrong : "transparent"};
  border-radius: ${({ theme }) => theme.radius.button};
  padding: ${({ theme }) => theme.spacing.sm};
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ theme }) => theme.colors.primary};
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:hover, &:focus-visible {
    border-color: ${({ theme }) => theme.colors.lineStrong};
    background: ${({ theme }) => theme.colors.surfaceAccent};
  }
`;

export const HistoryCopy = styled.span`
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  color: ${({ theme }) => theme.colors.text};
  font-size: 0.68rem;
  line-height: 1.5;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
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

export const ComposerFooter = styled.div`
  display: flex;
  min-width: 0;
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
  border-radius: ${({ theme }) => theme.radius.button};
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

export const CapabilityButton = styled.button<{ $active?: boolean }>`
  display: flex;
  align-items: center;
  gap: 0.3rem;
  border: 0;
  padding: 0.4rem;
  border-radius: ${({ theme }) => theme.radius.button};
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ theme, $active }) => $active ? theme.colors.primary : theme.colors.textSubtle};
  font: inherit;
  font-size: 0.7rem;
  cursor: pointer;
  &:hover:not(:disabled), &:focus-visible { background: ${({ theme }) => theme.colors.surfaceAccent}; color: ${({ theme }) => theme.colors.primary}; }
  &:disabled { cursor: not-allowed; opacity: 0.45; }
  @media (max-width: 38rem) { span { display: none; } }
`;

export const ComposerActions = styled.div`
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.35rem;
  margin-left: auto;
`;

export const HistoryButton = styled.button`
  display: grid;
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.button};
  background: ${({ theme }) => theme.colors.backgroundSoft};
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;

  &:hover:not(:disabled), &:focus-visible {
    border-color: ${({ theme }) => theme.colors.lineStrong};
    background: ${({ theme }) => theme.colors.surfaceAccent};
    color: ${({ theme }) => theme.colors.primary};
  }

  &:disabled { cursor: not-allowed; opacity: 0.35; }
`;

export const SendButton = styled.button`
  display: grid;
  width: 2.5rem;
  height: 2.5rem;
  flex: 0 0 auto;
  margin-left: auto;
  place-items: center;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.button};
  background: ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.background};
  cursor: pointer;
  &:hover:not(:disabled) { background: ${({ theme }) => theme.colors.primarySoft}; }
  &:disabled { cursor: not-allowed; opacity: 0.38; }
`;

export const StopButton = styled(SendButton)`
  border: 1px solid ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};

  &:hover:not(:disabled) {
    background: ${({ theme }) => theme.colors.accent};
    color: ${({ theme }) => theme.colors.background};
  }

  &:disabled { cursor: wait; }
`;
