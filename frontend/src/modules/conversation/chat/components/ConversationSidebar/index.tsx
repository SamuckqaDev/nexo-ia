import { Archive as ArchiveIcon, ChatCircleDots, Plus, ShieldCheck } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import type { Conversation } from "../../types/chatTypes";
import { Archive, Count, Empty, EmptyIcon, Header, Item, ItemMeta, List, NewButton, Open, Privacy, SectionLabel, Sidebar, Title } from "./styles";

type ConversationSidebarProps = {
  conversations: Conversation[];
  selectedId: string | null;
  isCreating: boolean;
  onSelect: (conversationId: string) => void;
  onNew: () => void;
  onArchive: (conversation: Conversation) => void;
};

export function ConversationSidebar({
  conversations,
  selectedId,
  isCreating,
  onSelect,
  onNew,
  onArchive
}: ConversationSidebarProps): ReactElement {
  return (
    <Sidebar aria-label="Conversations">
      <Header>
        <div>
          <Title>Conversations</Title>
          <Privacy><ShieldCheck size={13} weight="duotone" /> Private workspace</Privacy>
        </div>
        <Count>{conversations.length}</Count>
      </Header>

      <NewButton type="button" disabled={isCreating} onClick={onNew}>
        <Plus size={17} weight="bold" /> {isCreating ? "Creating…" : "New conversation"}
      </NewButton>

      <SectionLabel>Recent</SectionLabel>

      {conversations.length === 0 ? (
        <Empty>
          <EmptyIcon><ChatCircleDots size={22} weight="duotone" /></EmptyIcon>
          <strong>Your first idea starts here</strong>
          <span>Conversations stay private to your account.</span>
        </Empty>
      ) : (
        <List>
          {conversations.map((conversation: Conversation) => (
            <Item key={conversation.id} $active={conversation.id === selectedId}>
              <Open
                type="button"
                $active={conversation.id === selectedId}
                aria-current={conversation.id === selectedId}
                onClick={(): void => onSelect(conversation.id)}
              >
                <span>{conversation.title}</span>
                <ItemMeta>{conversation.selectedModel ?? "Model not selected"}</ItemMeta>
              </Open>
              <Archive
                type="button"
                aria-label={`Archive ${conversation.title}`}
                onClick={(): void => onArchive(conversation)}
              >
                <ArchiveIcon size={16} />
              </Archive>
            </Item>
          ))}
        </List>
      )}
    </Sidebar>
  );
}
