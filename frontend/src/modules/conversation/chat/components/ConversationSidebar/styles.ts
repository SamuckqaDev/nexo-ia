import styled from "styled-components";

export const Sidebar = styled.aside`
  z-index: 5;
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing.md};
  overflow: hidden;
  padding: ${({ theme }) => theme.spacing.md};
  border-right: 1px solid ${({ theme }) => theme.colors.line};
  background:
    radial-gradient(circle at 0 100%, ${({ theme }) => theme.colors.dangerSurface}, transparent 14rem),
    ${({ theme }) => theme.colors.surfaceAccent};

  @media (max-width: 48rem) {
    position: absolute;
    inset: 0 auto 0 0;
    width: min(20rem, calc(100% - 3.25rem));
    box-shadow: ${({ theme }) => theme.shadow};
  }
`;

export const DrawerScrim = styled.button`
  position: absolute;
  z-index: 4;
  inset: 0;
  display: none;
  border: 0;
  padding: 0;
  background: rgba(3, 11, 33, 0.68);
  backdrop-filter: blur(3px);

  @media (max-width: 48rem) { display: block; }
`;

export const Header = styled.header`
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: flex-start;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => `${theme.spacing.xs} 0 ${theme.spacing.sm}`};
`;

export const Title = styled.h2`margin: 0; font-size: 0.95rem;`;
export const Privacy = styled.span`
  display: flex;
  align-items: center;
  gap: 0.3rem;
  margin-top: 0.25rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.65rem;
`;
export const Count = styled.span`
  min-width: 1.8rem;
  padding: 0.28rem 0.45rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.round};
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.65rem;
  font-weight: 700;
  text-align: center;
`;
export const CollapseButton = styled.button`
  display: grid;
  width: 1.9rem;
  height: 1.9rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  cursor: pointer;
  &:hover, &:focus-visible { border-color: ${({ theme }) => theme.colors.lineStrong}; color: ${({ theme }) => theme.colors.primary}; }
`;
export const NewButton = styled.button`
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: ${({ theme }) => theme.spacing.xs};
  border: 1px solid ${({ theme }) => theme.colors.primary};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.72rem;
  background: ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.background};
  font: inherit;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  &:hover:not(:disabled) { background: ${({ theme }) => theme.colors.primarySoft}; }
  &:disabled { cursor: wait; opacity: 0.6; }
`;
export const SectionLabel = styled.span`
  padding: ${({ theme }) => `${theme.spacing.xs} 0 0.15rem`};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.11em;
  text-transform: uppercase;
`;
export const List = styled.div`
  display: grid;
  min-height: 0;
  align-content: start;
  gap: 0.4rem;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
`;
export const Item = styled.div<{ $active: boolean }>`
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.25rem;
  overflow: hidden;
  border: 1px solid ${({ theme, $active }) => $active ? theme.colors.lineStrong : theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceStrong : "transparent"};
  &:hover { border-color: ${({ theme }) => theme.colors.lineStrong}; background: ${({ theme }) => theme.colors.surfaceStrong}; }
  &::before { position: absolute; top: 0.55rem; bottom: 0.55rem; left: 0; width: 0.18rem; border-radius: ${({ theme }) => theme.radius.round}; background: ${({ theme, $active }) => $active ? theme.colors.primary : "transparent"}; content: ""; }
`;
export const Open = styled.button<{ $active: boolean }>`
  overflow: hidden;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.65rem 0.7rem;
  background: transparent;
  color: ${({ theme, $active }) => $active ? theme.colors.primarySoft : theme.colors.text};
  font: inherit;
  font-size: 0.82rem;
  font-weight: ${({ $active }) => $active ? 700 : 500};
  text-align: left;
  cursor: pointer;
  > span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: -2px; }
`;
export const ItemMeta = styled.small`
  display: block;
  overflow: hidden;
  margin-top: 0.22rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
`;
export const Activity = styled.small`
  display: flex;
  align-items: center;
  gap: 0.3rem;
  margin-top: 0.22rem;
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.62rem;
  font-weight: 700;
  svg { animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  @media (prefers-reduced-motion: reduce) { svg { animation: none; } }
`;
export const Archive = styled.button`
  display: flex;
  align-items: center;
  padding: 0.5rem;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  cursor: pointer;
  &:hover { color: ${({ theme }) => theme.colors.danger}; }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primary}; }
`;
export const Empty = styled.div`
  display: grid;
  justify-items: center;
  gap: 0.35rem;
  margin: ${({ theme }) => theme.spacing.lg} 0 0;
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px dashed ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.7rem;
  text-align: center;
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.76rem; }
`;
export const EmptyIcon = styled.span`
  display: grid;
  width: 2.5rem;
  height: 2.5rem;
  margin-bottom: 0.2rem;
  place-items: center;
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;
