import styled from "styled-components";

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
