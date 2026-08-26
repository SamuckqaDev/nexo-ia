import styled from "styled-components";

export const ActivityFeed = styled.section`
  display: grid;
  gap: 0;
`;

export const ActivitySummary = styled.header`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.5rem;
  margin-bottom: ${({ theme }) => theme.spacing.sm};
  padding: 0.65rem;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};

  > div { display: grid; gap: 0.12rem; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.68rem; }
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; line-height: 1.45; }
  .activity-spinner { animation: activity-spin 1s linear infinite; }
  @keyframes activity-spin { to { transform: rotate(360deg); } }
`;

export const ActivityState = styled.small`
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.49rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
`;

export const ActivityRow = styled.article<{ $tone: "running" | "success" | "danger" }>`
  display: grid;
  grid-template-columns: 1rem minmax(0, 1fr);
  gap: 0.5rem;
  min-width: 0;
  color: ${({ theme, $tone }) => $tone === "danger"
    ? theme.colors.danger
    : $tone === "running" ? theme.colors.primary : theme.colors.primarySoft};
`;

export const ActivityMarker = styled.span`
  position: relative;
  display: grid;
  justify-items: center;
  padding-top: 0.28rem;

  svg { position: relative; z-index: 1; background: ${({ theme }) => theme.colors.surfaceStrong}; }
  i {
    position: absolute;
    top: 1rem;
    bottom: -0.18rem;
    width: 1px;
    background: ${({ theme }) => theme.colors.lineStrong};
  }
  .activity-spinner { animation: activity-spin 1s linear infinite; }
  @keyframes activity-spin { to { transform: rotate(360deg); } }
`;

export const ActivityCopy = styled.div`
  display: grid;
  gap: 0.18rem;
  min-width: 0;
  padding: 0.2rem 0 0.78rem;
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};

  > span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; line-height: 1.45; }
  > small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.5rem; text-transform: capitalize; }
`;

export const ActivityHead = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.4rem;

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.62rem; }
  time { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.48rem; white-space: nowrap; }
`;

export const ActivityEvidence = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  padding-top: 0.18rem;

  span {
    display: inline-flex;
    align-items: center;
    gap: 0.22rem;
    max-width: 100%;
    padding: 0.2rem 0.34rem;
    border: 1px solid ${({ theme }) => theme.colors.line};
    border-radius: ${({ theme }) => theme.radius.button};
    color: ${({ theme }) => theme.colors.primarySoft};
    font-size: 0.48rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;
