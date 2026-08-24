import styled from "styled-components";

export const Panel = styled.aside`
  min-width: 0;
  padding: ${({ theme }) => theme.spacing.md};
  border-left: 1px solid ${({ theme }) => theme.colors.line};
  background: linear-gradient(180deg, ${({ theme }) => theme.colors.surfaceStrong}, ${({ theme }) => theme.colors.surface});
  overflow-y: auto;
`;

export const Rail = styled.aside`
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  padding: ${({ theme }) => `${theme.spacing.sm} 0.42rem`};
  border-left: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.surfaceStrong};
`;

export const RailButton = styled.button<{ $active?: boolean }>`
  display: grid;
  width: 2.45rem;
  height: 2.45rem;
  place-items: center;
  border: 1px solid ${({ $active, theme }) => $active ? theme.colors.lineStrong : "transparent"};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ $active, theme }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ $active, theme }) => $active ? theme.colors.primary : theme.colors.textSubtle};
  cursor: pointer;

  &:first-child {
    margin-bottom: ${({ theme }) => theme.spacing.sm};
    border-color: ${({ theme }) => theme.colors.line};
  }

  &:hover, &:focus-visible {
    border-color: ${({ theme }) => theme.colors.lineStrong};
    color: ${({ theme }) => theme.colors.primary};
  }
`;

export const PanelHeader = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: ${({ theme }) => theme.spacing.md};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.68rem;
`;

export const PanelTitle = styled.strong`
  display: block;
  margin-bottom: 0.16rem;
  color: ${({ theme }) => theme.colors.text};
  font-size: 0.82rem;
`;

export const CloseButton = styled.button`
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  cursor: pointer;

  &:hover, &:focus-visible {
    border-color: ${({ theme }) => theme.colors.lineStrong};
    color: ${({ theme }) => theme.colors.primary};
  }
`;

export const Tabs = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.2rem;
  padding: 0.2rem;
  margin-bottom: ${({ theme }) => theme.spacing.lg};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
`;

export const Tab = styled.button<{ $active: boolean }>`
  display: grid;
  gap: 0.25rem;
  place-items: center;
  min-width: 0;
  padding: 0.48rem 0.2rem;
  border: 0;
  border-radius: calc(${({ theme }) => theme.radius.control} - 0.18rem);
  background: ${({ $active, theme }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ $active, theme }) => $active ? theme.colors.primary : theme.colors.textSubtle};
  font-size: 0.61rem;
  font-weight: 700;
  cursor: pointer;

  &:hover { color: ${({ theme }) => theme.colors.primary}; }
`;

export const EmptyState = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  justify-items: start;
  padding: ${({ theme }) => theme.spacing.sm};
`;

export const EmptyIcon = styled.span`
  display: grid;
  width: 2.5rem;
  height: 2.5rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;

export const EmptyCopy = styled.div`
  display: grid;
  gap: 0.32rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.7rem;
  line-height: 1.55;

  strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 0.76rem;
  }
`;

export const PreviewBadge = styled.span`
  display: inline-flex;
  padding: 0.28rem 0.48rem;
  margin-bottom: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.round};
  color: ${({ theme }) => theme.colors.primary};
  font-size: 0.58rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
`;

export const ResourceList = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const ResourceCard = styled.section`
  display: grid;
  gap: 0.35rem;
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
  > header { display: flex; align-items: center; justify-content: space-between; gap: ${({ theme }) => theme.spacing.sm}; }
  > header > span { display: flex; min-width: 0; align-items: center; gap: 0.4rem; overflow: hidden; color: ${({ theme }) => theme.colors.text}; font-size: 0.68rem; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
  > header > small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.55rem; white-space: nowrap; }
`;

export const SourceButton = styled.button<{ $active: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.45rem;
  width: 100%;
  border: 1px solid ${({ theme, $active }) => $active ? theme.colors.lineStrong : "transparent"};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.48rem;
  background: ${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};
  color: ${({ theme, $active }) => $active ? theme.colors.primary : theme.colors.textSubtle};
  font: inherit;
  text-align: left;
  cursor: pointer;
  > span { display: grid; min-width: 0; gap: 0.1rem; }
  strong, small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.62rem; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.54rem; }
  &:hover:not(:disabled), &:focus-visible { border-color: ${({ theme }) => theme.colors.lineStrong}; background: ${({ theme }) => theme.colors.surfaceAccent}; }
  &:disabled { cursor: not-allowed; opacity: 0.55; }
`;

export const VaultSourceList = styled.div`
  display: grid;
  gap: 0.15rem;
  padding-top: 0.2rem;
`;

export const VaultSourceRow = styled.div`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.38rem;
  min-width: 0;
  padding: 0.28rem 0.42rem;
  color: ${({ theme }) => theme.colors.textSubtle};

  > span {
    overflow: hidden;
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.56rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  > small {
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.48rem;
    text-transform: capitalize;
  }
`;

export const StatusCopy = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  line-height: 1.55;
`;

export const SectionAction = styled.div`
  display: flex;
  justify-content: flex-end;
  padding-top: ${({ theme }) => theme.spacing.xs};
`;
