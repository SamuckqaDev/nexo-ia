import styled from "styled-components";

export const ArtifactStack = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const ArtifactCard = styled.article`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
`;

export const ArtifactHeader = styled.header`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};

  div { min-width: 0; }
  strong { display: block; overflow: hidden; color: ${({ theme }) => theme.colors.text}; font-size: 0.69rem; text-overflow: ellipsis; white-space: nowrap; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; }
`;

export const StatusBadge = styled.span`
  flex: 0 0 auto;
  padding: 0.22rem 0.4rem;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.button};
  color: ${({ theme }) => theme.colors.primary};
  font-size: 0.53rem;
  font-weight: 800;
  text-transform: lowercase;
`;

export const DiffGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.line};

  @media (max-width: 48rem) { grid-template-columns: 1fr; }
`;

export const DiffSide = styled.section<{ $after?: boolean }>`
  min-width: 0;
  background: ${({ $after, theme }) => $after ? theme.colors.surfaceAccent : theme.colors.surface};

  > span {
    display: block;
    padding: 0.38rem 0.48rem;
    border-bottom: 1px solid ${({ theme }) => theme.colors.line};
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.53rem;
    font-weight: 800;
    text-transform: uppercase;
  }

  pre {
    max-height: 16rem;
    padding: 0.5rem;
    margin: 0;
    overflow: auto;
    color: ${({ theme }) => theme.colors.textMuted};
    font: 0.58rem/1.55 ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    white-space: pre;
  }
`;

export const ArtifactActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const ArtifactNotice = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.58rem;
  line-height: 1.5;
`;
