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

  @media (max-width: 56rem) {
    padding-left: 3.35rem;

    &::before {
      position: absolute;
      z-index: 3;
      inset: 0 auto 0 0;
      width: 3.35rem;
      border-right: 1px solid ${({ theme }) => theme.colors.line};
      background: ${({ theme }) => theme.colors.surfaceStrong};
      content: "";
    }
  }
`;

export const MobileMenuButton = styled.button<{ $open: boolean }>`
  position: fixed;
  z-index: 50;
  top: 0.72rem;
  left: ${({ $open }) => $open ? "min(calc(100vw - 6.35rem), 15rem)" : "0.42rem"};
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
  transition: left 0.2s ease, color 0.15s ease, border-color 0.15s ease;

  &:hover,
  &:focus-visible {
    border-color: ${({ theme }) => theme.colors.primary};
    color: ${({ theme }) => theme.colors.primary};
  }

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
`;
