import styled from "styled-components";

export const Library = styled.div`height: 100%; min-height: 0; padding: ${({ theme }) => theme.spacing.md}; overflow: hidden;`;
export const TeamList = styled.div`display: grid; max-height: 100%; align-content: start; gap: 0.35rem; overflow: auto; overscroll-behavior: contain;`;
export const EmptyList = styled.div`height: 100%; overflow: auto;`;
export const TeamButton = styled.button<{ $active: boolean; $manageable: boolean }>`
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  width: 100%;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme, $active }) => $active ? theme.colors.lineStrong : "transparent"};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.7rem 0.7rem 0.7rem 0.9rem;
  overflow: hidden;
  background: ${({ theme, $active, $manageable }) => (!$active
    ? "transparent"
    : $manageable
      ? `linear-gradient(100deg, color-mix(in srgb, ${theme.colors.accent} 15%, transparent), ${theme.colors.surfaceAccent})`
      : theme.colors.surfaceAccent)};
  color: ${({ theme, $active, $manageable }) => ($active ? ($manageable ? theme.colors.accentSoft : theme.colors.primary) : theme.colors.textMuted)};
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  &::before {
    content: "";
    position: absolute;
    top: 0.5rem;
    bottom: 0.5rem;
    left: 0;
    width: 0.2rem;
    border-radius: ${({ theme }) => theme.radius.round};
    background: ${({ theme, $manageable }) => ($manageable ? theme.colors.accent : theme.colors.primary)};
    opacity: ${({ $active }) => ($active ? 1 : 0.45)};
  }
  &:hover, &:focus-visible { background: ${({ theme, $manageable }) => ($manageable
    ? `linear-gradient(100deg, color-mix(in srgb, ${theme.colors.accent} 13%, transparent), ${theme.colors.surfaceAccent})`
    : theme.colors.surfaceAccent)}; outline: none; }
`;
export const TeamCopy = styled.span`
  min-width: 0;
  strong, span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.7rem; }
  span { margin-top: 0.16rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.54rem; }
`;
