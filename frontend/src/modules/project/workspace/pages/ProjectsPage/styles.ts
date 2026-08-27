import styled from "styled-components";

export const ActiveContext = styled.section`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
  > div { min-width: 0; }
  span, strong, small { display: block; }
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.58rem; font-weight: 700; text-transform: uppercase; }
  strong { margin-top: 0.12rem; color: ${({ theme }) => theme.colors.text}; font-size: 0.82rem; }
  small { overflow: hidden; margin-top: 0.12rem; color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.62rem; text-overflow: ellipsis; white-space: nowrap; }
  @media (max-width: 46rem) { grid-template-columns: auto minmax(0, 1fr) auto; > button { grid-column: 1 / -1; width: 100%; } }
`;

export const ProjectsGrid = styled.div`
  display: grid;
  grid-template-columns: minmax(18rem, 0.8fr) minmax(0, 1.2fr);
  align-items: start;
  gap: ${({ theme }) => theme.spacing.lg};
  @media (max-width: 64rem) { grid-template-columns: 1fr; }
`;

export const StorageWarning = styled.p`
  margin: 0;
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.accent};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.68rem;
`;

export const Library = styled.div`display: grid; gap: ${({ theme }) => theme.spacing.md}; padding: ${({ theme }) => theme.spacing.md};`;
export const WorkspaceList = styled.div`display: grid; gap: ${({ theme }) => theme.spacing.sm};`;

export const WorkspaceButton = styled.button<{ $active: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 1px solid ${({ theme, $active }) => ($active ? theme.colors.primary : theme.colors.line)};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => theme.spacing.md};
  background: ${({ theme, $active }) => ($active ? theme.colors.surfaceAccent : theme.colors.background)};
  color: ${({ theme, $active }) => ($active ? theme.colors.primary : theme.colors.textMuted)};
  font: inherit;
  text-align: left;
  cursor: pointer;
  &:hover { border-color: ${({ theme }) => theme.colors.lineStrong}; }
`;

export const WorkspaceCopy = styled.span`
  display: grid;
  min-width: 0;
  gap: 0.14rem;
  strong, span, small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.74rem; }
  span { color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.6rem; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; }
`;

export const Detail = styled.div`display: grid; gap: ${({ theme }) => theme.spacing.lg}; padding: ${({ theme }) => theme.spacing.lg};`;

export const DetailHeader = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  > span { display: grid; width: 3.2rem; height: 3.2rem; place-items: center; border-radius: ${({ theme }) => theme.radius.control}; background: ${({ theme }) => theme.colors.surfaceAccent}; color: ${({ theme }) => theme.colors.primary}; }
  h2 { margin: 0.35rem 0 0; font-size: 1rem; }
`;

export const Path = styled.div`
  display: grid;
  gap: 0.35rem;
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.58rem; font-weight: 700; text-transform: uppercase; }
  code { overflow: auto; padding: ${({ theme }) => theme.spacing.sm}; border: 1px solid ${({ theme }) => theme.colors.line}; border-radius: ${({ theme }) => theme.radius.control}; background: ${({ theme }) => theme.colors.background}; color: ${({ theme }) => theme.colors.primarySoft}; font-size: 0.7rem; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.58rem; line-height: 1.5; }
`;

export const DetailMeta = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.sm};
  > div { display: flex; align-items: flex-start; gap: ${({ theme }) => theme.spacing.sm}; padding: ${({ theme }) => theme.spacing.sm}; border-radius: ${({ theme }) => theme.radius.control}; background: ${({ theme }) => theme.colors.background}; color: ${({ theme }) => theme.colors.primary}; }
  span { display: grid; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; }
  strong { margin-top: 0.15rem; color: ${({ theme }) => theme.colors.text}; font-size: 0.64rem; text-transform: capitalize; }
  @media (max-width: 38rem) { grid-template-columns: 1fr; }
`;

export const LocalBindings = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  > div { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: ${({ theme }) => theme.spacing.sm}; padding: ${({ theme }) => theme.spacing.sm}; border: 1px solid ${({ theme }) => theme.colors.line}; border-radius: ${({ theme }) => theme.radius.control}; color: ${({ theme }) => theme.colors.primary}; }
  span { display: grid; color: ${({ theme }) => theme.colors.text}; font-size: 0.66rem; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; }
`;

export const DetailActions = styled.div`display: flex; flex-wrap: wrap; justify-content: flex-end; gap: ${({ theme }) => theme.spacing.sm};`;

export const Structure = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const StructureHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};

  > div {
    display: grid;
    gap: 0.15rem;
  }

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.72rem; }
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.6rem; }
`;

export const StructureStatus = styled.p`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  margin: 0;
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px dashed ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.66rem;
`;
