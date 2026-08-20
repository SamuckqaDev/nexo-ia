import styled from "styled-components";

export const Shell = styled.div<{ $collapsed: boolean }>`
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-columns: ${({ $collapsed }) => ($collapsed ? "5rem" : "17rem")} minmax(0, 1fr);
  width: 100%;
  height: 100dvh;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: ${({ theme }) => theme.colors.background};
  transition: grid-template-columns 0.2s ease;

  @media (max-width: 56rem) {
    grid-template-columns: 1fr;
  }
`;

export const Sidebar = styled.aside<{ $collapsed: boolean; $mobileOpen: boolean }>`
  position: sticky;
  z-index: 10;
  top: 0;
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  padding: ${({ theme, $collapsed }) => ($collapsed ? theme.spacing.md : theme.spacing.lg)};
  border-right: 1px solid ${({ theme }) => theme.colors.line};
  background:
    radial-gradient(circle at 50% 0, ${({ theme }) => theme.colors.surfaceAccent}, transparent 14rem),
    radial-gradient(circle at 0 100%, ${({ theme }) => theme.colors.dangerSurface}, transparent 12rem),
    ${({ theme }) => theme.colors.surfaceStrong};
  transition: padding 0.2s ease;
  overflow: visible;

  @media (max-width: 56rem) {
    position: fixed;
    z-index: 40;
    inset: 0 auto 0 0;
    width: min(18rem, calc(100vw - 3.5rem));
    padding: ${({ theme }) => theme.spacing.lg};
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
    transform: translateX(${({ $mobileOpen }) => ($mobileOpen ? "0" : "-105%")});
    transition: transform 0.2s ease;
  }
`;

export const Brand = styled.div`
  position: relative;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  margin-bottom: ${({ theme }) => theme.spacing.md};
  padding-bottom: ${({ theme }) => theme.spacing.md};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  font-size: 1.05rem;
  font-weight: 700;

  &::after {
    position: absolute;
    right: 0;
    bottom: -1px;
    left: 0;
    height: 1px;
    background: linear-gradient(90deg, ${({ theme }) => theme.colors.primary}, ${({ theme }) => theme.colors.accent}, transparent 85%);
    content: "";
  }
`;

export const BrandName = styled.span<{ $hidden: boolean }>`
  display: ${({ $hidden }) => ($hidden ? "none" : "inline")};
  white-space: nowrap;
`;

export const Logo = styled.img`
  width: 2.4rem;
  height: 2.4rem;
  object-fit: contain;
`;

export const EdgeToggle = styled.button<{ $collapsed?: boolean }>`
  position: absolute;
  top: 5.4rem;
  right: -0.85rem;
  z-index: 6;
  display: grid;
  width: 1.7rem;
  height: 1.7rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.round};
  background: ${({ theme }) => theme.colors.backgroundElevated};
  box-shadow: ${({ theme }) => theme.shadow};
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease;

  &:hover {
    border-color: ${({ theme }) => theme.colors.primary};
    color: ${({ theme }) => theme.colors.primary};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }

  @media (max-width: 56rem) {
    display: none;
  }
`;

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
  border: 1px solid ${({ theme, $active }) => ($active ? theme.colors.lineStrong : theme.colors.line)};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.55rem 0.7rem;
  background: ${({ theme, $active }) => ($active ? theme.colors.surfaceAccent : "transparent")};
  color: ${({ theme, $active }) => ($active ? theme.colors.primarySoft : theme.colors.textMuted)};
  font: inherit;
  font-size: 0.82rem;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
  position: relative;
  overflow: hidden;

  &::before {
    position: absolute;
    top: 0.4rem;
    bottom: 0.4rem;
    left: 0;
    width: 0.2rem;
    border-radius: ${({ theme }) => theme.radius.round};
    background: ${({ theme, $active }) => ($active ? `linear-gradient(${theme.colors.primary}, ${theme.colors.accent})` : "transparent")};
    content: "";
  }

  &:hover {
    border-color: ${({ theme }) => theme.colors.lineStrong};
    color: ${({ theme }) => theme.colors.primary};
    background: ${({ theme }) => theme.colors.surface};
  }
`;

export const NavLabel = styled.span<{ $hidden: boolean }>`
  display: ${({ $hidden }) => ($hidden ? "none" : "inline")};
`;

export const Workspace = styled.div`
  position: relative;
  z-index: 0;
  display: grid;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  isolation: isolate;
`;

export const MobileMenuButton = styled.button`
  position: fixed;
  z-index: 50;
  top: ${({ theme }) => theme.spacing.md};
  left: ${({ theme }) => theme.spacing.md};
  display: none;
  width: 2.7rem;
  height: 2.7rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0;
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};
  color: ${({ theme }) => theme.colors.primarySoft};
  cursor: pointer;

  @media (max-width: 56rem) {
    display: grid;
  }
`;

export const MobileScrim = styled.button`
  position: fixed;
  z-index: 30;
  inset: 0;
  display: none;
  border: 0;
  padding: 0;
  background: rgba(3, 11, 33, 0.7);
  backdrop-filter: blur(4px);

  @media (max-width: 56rem) {
    display: block;
  }
`;

export const Main = styled.main`
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;

  @media (max-width: 56rem) {
    padding-top: 4.5rem;
  }
`;
