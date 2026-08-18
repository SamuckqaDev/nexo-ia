import { Archive as ArchiveIcon, Plus } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type { Conversation } from "../../types/chatTypes";
import { Archive, Empty, Item, List, Open, Sidebar } from "./styles";

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
      <Button type="button" icon={Plus} disabled={isCreating} onClick={onNew}>
        New chat
      </Button>

      {conversations.length === 0 ? (
        <Empty>Your conversations stay private to your account.</Empty>
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
                {conversation.title}
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
