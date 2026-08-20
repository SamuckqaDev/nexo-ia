import styled from "styled-components";

export const Layout = styled.section`
  display: grid;
  grid-template-columns: 15rem minmax(0, 1fr);
  width: 100%;
  height: 100dvh;
  background:
    radial-gradient(circle at 85% 0, ${({ theme }) => theme.colors.surfaceAccent}, transparent 26rem),
    ${({ theme }) => theme.colors.surfaceStrong};
  overflow: hidden;

  @media (max-width: 56rem) {
    height: calc(100dvh - 4.5rem);
  }

  @media (max-width: 48rem) {
    grid-template-columns: 1fr;
  }
`;

export const Chat = styled.div`
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  background: linear-gradient(180deg, ${({ theme }) => theme.colors.surface} 0, transparent 8rem);
`;

export const ConversationBody = styled.div<{ $contextOpen: boolean }>`
  position: relative;
  display: grid;
  grid-template-columns: ${({ $contextOpen }) => $contextOpen ? "minmax(0, 1fr) 17rem" : "minmax(0, 1fr) 3.5rem"};
  min-width: 0;
  min-height: 0;

  @media (max-width: 78rem) {
    grid-template-columns: ${({ $contextOpen }) => $contextOpen ? "minmax(0, 1fr) 14.5rem" : "minmax(0, 1fr) 3.5rem"};
  }

  @media (max-width: 64rem) {
    grid-template-columns: ${({ $contextOpen }) => $contextOpen ? "minmax(0, 1fr)" : "minmax(0, 1fr) 3.5rem"};

    > [aria-label="Conversation context"] {
      position: absolute;
      z-index: 2;
      inset: 0 0 0 auto;
      width: min(18rem, 92%);
      box-shadow: ${({ theme }) => theme.shadow};
    }
  }
`;

export const ConversationColumn = styled.div`
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  min-width: 0;
  min-height: 0;
`;

export const Header = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  min-height: 3.35rem;
  padding: ${({ theme }) => `${theme.spacing.xs} ${theme.spacing.md}`};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};

  @media (max-width: 48rem) {
    flex-wrap: wrap;
  }
`;

export const HeaderCopy = styled.div`
  display: flex;
  min-width: 0;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const HeaderTitle = styled.h2`
  overflow: hidden;
  margin: 0;
  font-size: 0.84rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const HeaderMeta = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  span { display: flex; align-items: center; gap: 0.25rem; }
`;

export const PrivacyBadge = styled.span`
  color: ${({ theme }) => theme.colors.accentSoft};
`;

export const ModelArea = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const LoadFailure = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  justify-items: center;
  margin: auto;
  padding: ${({ theme }) => theme.spacing.xl};
  color: ${({ theme }) => theme.colors.textMuted};
  text-align: center;
`;
