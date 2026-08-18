import styled from "styled-components";

export const Shell = styled.div<{ $collapsed: boolean }>`
  display: grid;
  grid-template-columns: ${({ $collapsed }) => ($collapsed ? "5rem" : "17rem")} minmax(0, 1fr);
  min-height: 100vh;
  background: ${({ theme }) => theme.colors.background};
  transition: grid-template-columns 0.2s ease;

  @media (max-width: 56rem) {
    grid-template-columns: 1fr;
  }
`;

export const Sidebar = styled.aside<{ $collapsed: boolean }>`
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  flex-direction: column;
  padding: ${({ theme, $collapsed }) => ($collapsed ? theme.spacing.md : theme.spacing.lg)};
  border-right: 1px solid ${({ theme }) => theme.colors.line};
  background:
    radial-gradient(circle at 50% 0, ${({ theme }) => theme.colors.surfaceAccent}, transparent 14rem),
    radial-gradient(circle at 0 100%, ${({ theme }) => theme.colors.dangerSurface}, transparent 12rem),
    ${({ theme }) => theme.colors.surfaceStrong};
  transition: padding 0.2s ease;

  @media (max-width: 56rem) {
    display: none;
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

export const SidebarToggle = styled.button<{ $collapsed?: boolean }>`
  display: flex;
  align-items: center;
  justify-content: ${({ $collapsed }) => ($collapsed ? "center" : "flex-start")};
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  margin-top: auto;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.75rem;
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.textMuted};
  font: inherit;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;

  &:hover {
    border-color: ${({ theme }) => theme.colors.lineStrong};
    color: ${({ theme }) => theme.colors.primary};
  }

  @media (max-width: 56rem) {
    display: none;
  }
`;

export const Navigation = styled.nav`
  display: grid;
  gap: 0.25rem;
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
  min-width: 0;
`;

export const Header = styled.header`
  position: relative;
  z-index: 10;
  display: flex;
  min-height: 3.5rem;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.xs} ${({ theme }) => theme.spacing.xl};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.surface};
  backdrop-filter: blur(18px);

  @media (max-width: 40rem) {
    padding: ${({ theme }) => theme.spacing.xs} ${({ theme }) => theme.spacing.md};
  }
`;

export const MobileMenuButton = styled(SidebarToggle)`
  position: static;
  transform: none;
  display: none;
  width: 2.7rem;
  height: 2.7rem;
  align-items: center;
  justify-content: center;
  margin-left: 0;
  margin-top: 0;
  padding: 0;

  @media (max-width: 56rem) {
    display: grid;
  }
`;

export const MobileNavigation = styled.nav<{ $open: boolean }>`
  position: absolute;
  top: calc(100% + 1px);
  left: ${({ theme }) => theme.spacing.md};
  display: ${({ $open }) => ($open ? "grid" : "none")};
  width: min(20rem, calc(100vw - 2rem));
  gap: 0.35rem;
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};

  @media (min-width: 56.01rem) {
    display: none;
  }
`;

export const HeaderIdentity = styled.div`
  display: flex;
  min-width: 0;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const HeaderTitle = styled.div`
  min-width: 0;

  strong {
    display: block;
  }

  span {
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.75rem;
  }

  @media (max-width: 32rem) {
    span {
      display: none;
    }
  }
`;

export const HeaderActions = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};

  @media (max-width: 32rem) {
    gap: ${({ theme }) => theme.spacing.xs};
  }
`;

export const IconButton = styled.button`
  display: grid;
  width: 2.3rem;
  height: 2.3rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;

  &:hover {
    border-color: ${({ theme }) => theme.colors.lineStrong};
    color: ${({ theme }) => theme.colors.primary};
  }
`;

export const NotificationButton = styled(IconButton)`
  &:hover {
    border-color: ${({ theme }) => theme.colors.accent};
    color: ${({ theme }) => theme.colors.accentSoft};
  }
`;

export const Main = styled.main<{ $wide?: boolean }>`
  width: ${({ $wide }) => $wide ? "100%" : "min(78rem, 100%)"};
  margin: 0 auto;
  padding: ${({ $wide, theme }) => $wide ? theme.spacing.lg : theme.spacing.xl};

  @media (max-width: 40rem) {
    padding: ${({ theme }) => theme.spacing.lg};
  }
`;
