import styled from "styled-components";

export const Layout = styled.section<{ $sidebarOpen: boolean }>`
  position: relative;
  display: grid;
  grid-template-columns: ${({ $sidebarOpen }) => $sidebarOpen ? "15rem minmax(0, 1fr)" : "minmax(0, 1fr)"};
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  background:
    radial-gradient(circle at 85% 0, ${({ theme }) => theme.colors.surfaceAccent}, transparent 26rem),
    ${({ theme }) => theme.colors.surfaceStrong};
  overflow: hidden;

  @media (max-width: 48rem) {
    grid-template-columns: 1fr;
  }
`;

export const Chat = styled.div`
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  background: linear-gradient(180deg, ${({ theme }) => theme.colors.surface} 0, transparent 8rem);
`;

export const ChatContent = styled.div`
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
`;

export const ConversationBody = styled.div<{ $contextOpen: boolean }>`
  position: relative;
  display: grid;
  grid-template-columns: ${({ $contextOpen }) => $contextOpen ? "minmax(0, 1fr) 20rem" : "minmax(0, 1fr) 3.5rem"};
  min-width: 0;
  min-height: 0;
  flex: 1;

  @media (max-width: 78rem) {
    grid-template-columns: ${({ $contextOpen }) => $contextOpen ? "minmax(0, 1fr) 18rem" : "minmax(0, 1fr) 3.5rem"};
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
  grid-template-rows: minmax(0, 1fr) auto auto;
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

  @media (max-width: 28rem) {
    align-items: stretch;
  }
`;

export const HeaderLeading = styled.div`
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  overflow: hidden;
`;

export const OpenConversations = styled.button`
  display: grid;
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0;
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primarySoft};
  cursor: pointer;
  &:hover, &:focus-visible { border-color: ${({ theme }) => theme.colors.primary}; color: ${({ theme }) => theme.colors.primary}; }
`;

export const HeaderCopy = styled.div`
  display: flex;
  flex: 1;
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

export const TitleEdit = styled.div`
  display: flex;
  min-width: 0;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const TitleInput = styled.input`
  width: min(18rem, 35vw);
  min-width: 8rem;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.45rem 0.6rem;
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
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

export const NewConversationWorkspace = styled.section`
  display: grid;
  width: min(34rem, 100%);
  box-sizing: border-box;
  justify-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  margin-top: ${({ theme }) => theme.spacing.lg};
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceAccent};

  > div:first-child {
    display: flex;
    align-items: center;
    gap: ${({ theme }) => theme.spacing.sm};
    color: ${({ theme }) => theme.colors.primary};
    text-align: left;
  }

  span { display: grid; gap: 0.15rem; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.76rem; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.62rem; line-height: 1.45; }
`;

export const WorkspaceServerNotice = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  width: min(60rem, calc(100% - 2rem));
  box-sizing: border-box;
  justify-self: center;
  padding: ${({ theme }) => theme.spacing.sm} ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.64rem;
  > span { display: grid; gap: 0.12rem; }
  strong { color: ${({ theme }) => theme.colors.text}; text-transform: capitalize; }
`;

export const ModelArea = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};

  @media (max-width: 28rem) {
    width: 100%;
    > div { width: 100%; justify-items: stretch; }
  }
`;

export const ModelLockNotice = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  max-width: 13rem;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.round};
  padding: 0.28rem 0.5rem;
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.58rem;
  font-weight: 700;
  white-space: nowrap;
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

export const VaultBar = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  width: min(60rem, calc(100% - 2rem));
  box-sizing: border-box;
  justify-self: center;
  padding: 0 ${({ theme }) => theme.spacing.md} ${({ theme }) => theme.spacing.xs};
`;

export const VaultBarLabel = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
`;

export const VaultChip = styled.button<{ $active: boolean }>`
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: 1px solid ${({ theme, $active }) => ($active ? theme.colors.primary : theme.colors.line)};
  border-radius: ${({ theme }) => theme.radius.round};
  padding: 0.28rem 0.65rem;
  background: ${({ theme, $active }) => ($active ? theme.colors.surfaceAccent : "transparent")};
  color: ${({ theme, $active }) => ($active ? theme.colors.primarySoft : theme.colors.textMuted)};
  font: inherit;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;

  > span { display: inline-flex; align-items: center; gap: 0.28rem; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.55rem; font-weight: 700; }
  small::before { content: "·"; margin-right: 0.28rem; }

  &:hover { border-color: ${({ theme }) => theme.colors.lineStrong}; }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: 1px; }
`;
