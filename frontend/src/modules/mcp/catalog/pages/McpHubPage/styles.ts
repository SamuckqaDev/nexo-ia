import styled from "styled-components";

export const HubLayout = styled.div`
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr);
  gap: ${({ theme }) => theme.spacing.sm};
  overflow: hidden;

  @media (max-width: 48rem) { overflow: auto; }
`;

export const Worlds = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.sm};

  > span {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 0.45rem;
    border: 1px solid ${({ theme }) => theme.colors.line};
    border-radius: ${({ theme }) => theme.radius.control};
    padding: 0.55rem 0.7rem;
    background: ${({ theme }) => theme.colors.surface};
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.62rem;
  }

  svg { flex: 0 0 auto; color: ${({ theme }) => theme.colors.primary}; }
  strong { color: ${({ theme }) => theme.colors.text}; }

  @media (max-width: 48rem) { grid-template-columns: 1fr; }
`;

export const HubGrid = styled.div`
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(15rem, 0.9fr) minmax(14rem, 0.75fr) minmax(20rem, 1.35fr);
  gap: ${({ theme }) => theme.spacing.md};
  overflow: hidden;

  > section {
    display: grid;
    min-height: 0;
    grid-template-rows: auto minmax(0, 1fr);
  }

  > section > div:last-child { min-height: 0; overflow: hidden; }

  @media (max-width: 72rem) {
    grid-template-columns: minmax(14rem, 0.9fr) minmax(18rem, 1.1fr);
    > section:last-child { grid-column: 1 / -1; }
  }

  @media (max-width: 48rem) {
    height: auto;
    grid-template-columns: 1fr;
    overflow: auto;
    > section, > section:last-child { min-height: 24rem; grid-column: auto; }
  }
`;

export const PanelContent = styled.div`
  display: grid;
  height: 100%;
  min-height: 0;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.md};
  overflow: hidden;
`;

export const CatalogList = styled.div`
  display: grid;
  min-height: 0;
  align-content: start;
  gap: ${({ theme }) => theme.spacing.sm};
  overflow: auto;
  overscroll-behavior: contain;
`;

export const CatalogCard = styled.article`
  display: grid;
  gap: 0.55rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.75rem;
  background: ${({ theme }) => theme.colors.background};

  > p {
    display: -webkit-box;
    margin: 0;
    overflow: hidden;
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.62rem;
    line-height: 1.5;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
  }

  > button { justify-self: start; max-width: 100%; }
`;

export const ServerIdentity = styled.div`
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.55rem;
  color: ${({ theme }) => theme.colors.primary};

  > div { min-width: 0; }
  strong, span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.72rem; }
  span { margin-top: 0.12rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; text-transform: capitalize; }
`;

export const CatalogMeta = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.56rem;
`;

export const ConnectionList = styled.div`
  display: grid;
  height: 100%;
  min-height: 0;
  align-content: start;
  gap: 0.3rem;
  padding: ${({ theme }) => theme.spacing.sm};
  overflow: auto;
  overscroll-behavior: contain;
`;

export const ConnectionButton = styled.button<{ $active: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 1px solid ${({ theme, $active }) => $active ? theme.colors.lineStrong : "transparent"};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.7rem;
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ theme, $active }) => $active ? theme.colors.primary : theme.colors.textMuted};
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:hover { background: ${({ theme }) => theme.colors.surfaceAccent}; }
`;

export const ConnectionCopy = styled.span`
  min-width: 0;
  strong, span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.69rem; }
  span { margin-top: 0.16rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; }
`;

export const DetailBody = styled.div`
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
`;

export const DetailHeader = styled.header`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.md};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};

  > div:first-child { display: grid; min-width: 0; gap: 0.28rem; }
  span, small { overflow: hidden; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.58rem; text-overflow: ellipsis; white-space: nowrap; }

  @media (max-width: 40rem) { flex-direction: column; }
`;

export const DetailActions = styled.div`
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacing.xs};
  padding: ${({ theme }) => theme.spacing.sm};
`;

export const InlineNotice = styled.div`
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: ${({ theme }) => theme.spacing.sm};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.65rem;
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.62rem;
  line-height: 1.45;
`;

export const ToolList = styled.div`
  display: grid;
  flex: 1;
  min-height: 0;
  align-content: start;
  overflow: auto;
  overscroll-behavior: contain;
`;

export const ToolButton = styled.button<{ $active: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 0;
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  padding: ${({ theme }) => theme.spacing.md};
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ theme, $active }) => $active ? theme.colors.primary : theme.colors.textMuted};
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:hover { background: ${({ theme }) => theme.colors.surfaceAccent}; }
`;

export const ToolCopy = styled.span`
  min-width: 0;
  strong, code, p { display: block; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.68rem; }
  code { margin-top: 0.18rem; color: ${({ theme }) => theme.colors.primarySoft}; font-size: 0.55rem; overflow-wrap: anywhere; }
  p { display: -webkit-box; margin: 0.35rem 0 0; overflow: hidden; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.58rem; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
`;

export const EmptyTools = styled.div`
  display: grid;
  min-height: 12rem;
  place-content: center;
  justify-items: center;
  gap: 0.35rem;
  padding: ${({ theme }) => theme.spacing.lg};
  color: ${({ theme }) => theme.colors.primary};
  text-align: center;

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.75rem; }
  span { max-width: 24rem; color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.62rem; line-height: 1.5; }
`;
