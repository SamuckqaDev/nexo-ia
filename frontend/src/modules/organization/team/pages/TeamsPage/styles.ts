import styled from "styled-components";

export const Workspace = styled.div`
  display: grid;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(13rem, 0.48fr) minmax(0, 1.8fr);
  gap: ${({ theme }) => theme.spacing.lg};
  overflow: hidden;

  > section { display: grid; min-height: 0; grid-template-rows: auto minmax(0, 1fr); }
  > section > div:last-child { min-height: 0; overflow: hidden; }

  @media (max-width: 52rem) {
    height: auto;
    grid-template-columns: 1fr;
    align-content: start;
    overflow: auto;
    > section:first-child { min-height: 15rem; max-height: 22rem; }
    > section:last-child { min-height: 32rem; }
  }
`;

export const PageActions = styled.div`display: flex; gap: ${({ theme }) => theme.spacing.sm};`;
export const Library = styled.div`height: 100%; min-height: 0; padding: ${({ theme }) => theme.spacing.md}; overflow: hidden;`;
export const TeamList = styled.div`display: grid; max-height: 100%; align-content: start; gap: 0.35rem; overflow: auto; overscroll-behavior: contain;`;
export const EmptyList = styled.div`height: 100%; overflow: auto;`;
export const TeamButton = styled.button<{ $active: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  width: 100%;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme, $active }) => $active ? theme.colors.lineStrong : "transparent"};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.7rem;
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ theme, $active }) => $active ? theme.colors.primary : theme.colors.textMuted};
  font: inherit;
  text-align: left;
  cursor: pointer;
  &:hover, &:focus-visible { background: ${({ theme }) => theme.colors.surfaceAccent}; outline: none; }
`;
export const TeamCopy = styled.span`
  min-width: 0;
  strong, span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.7rem; }
  span { margin-top: 0.16rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.54rem; }
`;
export const Detail = styled.div`height: 100%; min-height: 0; overflow: hidden;`;
export const DetailScroll = styled.div`display: grid; max-height: 100%; gap: ${({ theme }) => theme.spacing.lg}; padding: ${({ theme }) => theme.spacing.md}; overflow: auto; overscroll-behavior: contain;`;
export const MetaGrid = styled.div`display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: ${({ theme }) => theme.spacing.sm}; @media (max-width: 62rem) { grid-template-columns: repeat(2, minmax(0, 1fr)); } @media (max-width: 34rem) { grid-template-columns: 1fr; }`;
export const MetaItem = styled.div`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  column-gap: 0.48rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.7rem;
  background: ${({ theme }) => theme.colors.background};
  color: ${({ theme }) => theme.colors.primary};
  span, strong { grid-column: 2; }
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.52rem; text-transform: uppercase; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.66rem; text-transform: capitalize; }
`;
export const SectionHeading = styled.header`
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.md};
  h3 { margin: 0; font-size: 0.82rem; }
  p { margin: 0.22rem 0 0; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.6rem; line-height: 1.5; }
`;
export const MemberGrid = styled.div`display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: ${({ theme }) => theme.spacing.sm}; @media (max-width: 50rem) { grid-template-columns: 1fr; }`;
export const MemberCard = styled.article`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.72rem;
  background: ${({ theme }) => theme.colors.background};
  > span { min-width: 0; }
  strong, small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { font-size: 0.68rem; }
  small { margin-top: 0.18rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.52rem; }
  > div { display: flex; flex: 0 0 auto; gap: 0.3rem; }
  @media (max-width: 34rem) { align-items: stretch; flex-direction: column; }
`;
export const SharedVaultGrid = styled.div`display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: ${({ theme }) => theme.spacing.sm}; @media (max-width: 50rem) { grid-template-columns: 1fr; }`;
export const SharedVault = styled.article`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.72rem;
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
  span { min-width: 0; }
  strong, small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.68rem; }
  small { margin-top: 0.18rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.52rem; }
`;
export const AdminArea = styled.section`display: grid; gap: ${({ theme }) => theme.spacing.md}; border-top: 1px solid ${({ theme }) => theme.colors.line}; padding-top: ${({ theme }) => theme.spacing.lg};`;
