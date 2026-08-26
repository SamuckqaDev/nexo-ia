import styled from "styled-components";

export const Navigation = styled.nav`
  display: grid;
  min-height: 0;
  flex: 1;
  align-content: start;
  gap: 0.25rem;
  overflow-y: auto;
  overscroll-behavior: contain;
`;

export const NavigationLabel = styled.span<{ $hidden: boolean }>`
  display: ${({ $hidden }) => ($hidden ? "none" : "block")};
  margin: 0 0 ${({ theme }) => theme.spacing.xs};
  padding: 0 0.25rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
`;

export const NavButton = styled.button<{ $active: boolean; $collapsed: boolean }>`
  display: flex;
  align-items: center;
  justify-content: ${({ $collapsed }) => ($collapsed ? "center" : "flex-start")};
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 1px solid ${({ theme, $active }) => ($active ? theme.colors.lineStrong : "transparent")};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.58rem 0.7rem;
  background: ${({ theme, $active }) => ($active
    ? `linear-gradient(100deg, color-mix(in srgb, ${theme.colors.primary} 20%, transparent), color-mix(in srgb, ${theme.colors.accent} 12%, transparent))`
    : "transparent")};
  color: ${({ theme, $active }) => ($active ? theme.colors.text : theme.colors.textMuted)};
  font: inherit;
  font-size: 0.82rem;
  font-weight: ${({ $active }) => ($active ? 700 : 600)};
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  transition: color 0.15s ease, background 0.15s ease, border-color 0.15s ease;

  &::after {
    content: ${({ $active, $collapsed }) => ($active && !$collapsed ? '""' : "none")};
    width: 0.42rem;
    height: 0.42rem;
    margin-left: auto;
    border-radius: ${({ theme }) => theme.radius.round};
    background: ${({ theme }) => theme.colors.primary};
    box-shadow: 0 0 0.55rem ${({ theme }) => theme.colors.primary};
  }

  &:hover {
    color: ${({ theme, $active }) => ($active ? theme.colors.text : theme.colors.primary)};
    background: ${({ theme, $active }) => ($active
      ? `linear-gradient(100deg, color-mix(in srgb, ${theme.colors.primary} 24%, transparent), color-mix(in srgb, ${theme.colors.accent} 14%, transparent))`
      : theme.colors.surface)};
  }
`;

export const NavLabel = styled.span<{ $hidden: boolean }>`
  display: ${({ $hidden }) => ($hidden ? "none" : "inline")};
`;

export const NavDivider = styled.div<{ $hidden: boolean }>`
  height: 1px;
  margin: ${({ $hidden }) => ($hidden ? "0.35rem 0.75rem" : "0.5rem 0.6rem")};
  background: ${({ theme }) => theme.colors.line};
`;
