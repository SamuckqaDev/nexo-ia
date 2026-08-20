import {
  Archive as ArchiveIcon,
  CaretDoubleLeft,
  CaretDoubleRight,
  ChatCircleDots,
  Plus,
  ShieldCheck
} from "@phosphor-icons/react";
import { Fragment, type ReactElement } from "react";
import type { Conversation } from "../../types/chatTypes";
import {
  Archive,
  CollapseButton,
  Count,
  DrawerScrim,
  Empty,
  EmptyIcon,
  Header,
  Item,
  ItemMeta,
  List,
  NewButton,
  Open,
  Privacy,
  Rail,
  RailButton,
  SectionLabel,
  Sidebar,
  Title
} from "./styles";

type ConversationSidebarProps = {
  conversations: Conversation[];
  selectedId: string | null;
  isCreating: boolean;
  open: boolean;
  onSelect: (conversationId: string) => void;
  onNew: () => void;
  onArchive: (conversation: Conversation) => void;
  onOpenChange: (open: boolean) => void;
};

const isCompactViewport = (): boolean => typeof window !== "undefined"
  && typeof window.matchMedia === "function"
  && window.matchMedia("(max-width: 48rem)").matches;

export function ConversationSidebar({
  conversations,
  selectedId,
  isCreating,
  open,
  onSelect,
  onNew,
  onArchive,
  onOpenChange
}: ConversationSidebarProps): ReactElement {
  if (!open) {
    return (
      <Rail aria-label="Conversation menu">
        <RailButton
          type="button"
          aria-label="Expand conversation menu"
          title="Expand conversations"
          onClick={(): void => onOpenChange(true)}
        >
          <CaretDoubleRight size={17} />
        </RailButton>
        <RailButton
          type="button"
          aria-label="New conversation"
          title="New conversation"
          disabled={isCreating}
          onClick={onNew}
        >
          <Plus size={18} weight="bold" />
        </RailButton>
        <Count title={`${conversations.length} conversations`}>{conversations.length}</Count>
      </Rail>
    );
  }

  return (
    <Fragment>
      <DrawerScrim
        type="button"
        aria-label="Close conversation menu"
        onClick={(): void => onOpenChange(false)}
      />
      <Sidebar aria-label="Conversations">
        <Header>
          <div>
            <Title>Conversations</Title>
            <Privacy><ShieldCheck size={13} weight="duotone" /> Private workspace</Privacy>
          </div>
          <Count>{conversations.length}</Count>
          <CollapseButton
            type="button"
            aria-label="Minimize conversation menu"
            title="Minimize conversations"
            onClick={(): void => onOpenChange(false)}
          >
            <CaretDoubleLeft size={16} />
          </CollapseButton>
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
                  aria-label={`Open ${conversation.title}`}
                  onClick={(): void => {
                    onSelect(conversation.id);
                    if (isCompactViewport()) onOpenChange(false);
                  }}
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
    </Fragment>
  );
}
