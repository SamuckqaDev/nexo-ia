import {
  Archive as ArchiveIcon,
  CaretDoubleLeft,
  ChatCircleDots,
  Plus,
  ShieldCheck,
  SpinnerGap
} from "@phosphor-icons/react";
import { Fragment, type ReactElement } from "react";
import { useChatStreamStore } from "../../stores/useChatStreamStore";
import type { ChatStreamState, ConversationStreamSnapshot } from "../../types/chatStreamTypes";
import type { Conversation } from "../../types/chatTypes";
import {
  Activity,
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
  SectionLabel,
  Sidebar,
  Title
} from "./styles";

type ConversationSidebarProps = {
  conversations: Conversation[];
  selectedId: string | null;
  isCreating: boolean;
  onSelect: (conversationId: string) => void;
  onNew: () => void;
  onArchive: (conversation: Conversation) => void;
  onClose: () => void;
};

const isCompactViewport = (): boolean => typeof window !== "undefined"
  && typeof window.matchMedia === "function"
  && window.matchMedia("(max-width: 48rem)").matches;

export function ConversationSidebar({
  conversations,
  selectedId,
  isCreating,
  onSelect,
  onNew,
  onArchive,
  onClose
}: ConversationSidebarProps): ReactElement {
  const streams: Record<string, ConversationStreamSnapshot> = useChatStreamStore(
    (state: ChatStreamState) => state.streams);

  return (
    <Fragment>
      <DrawerScrim
        type="button"
        aria-label="Close conversation menu"
        onClick={onClose}
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
            onClick={onClose}
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
            {conversations.map((conversation: Conversation) => {
              const phase = streams[conversation.id]?.phase;
              const isRunning: boolean = phase === "starting" || phase === "streaming" || phase === "cancelling";
              return (
              <Item key={conversation.id} $active={conversation.id === selectedId}>
                <Open
                  type="button"
                  $active={conversation.id === selectedId}
                  aria-current={conversation.id === selectedId}
                  aria-label={`Open ${conversation.title}`}
                  onClick={(): void => {
                    onSelect(conversation.id);
                    if (isCompactViewport()) onClose();
                  }}
                >
                  <span>{conversation.title}</span>
                  {isRunning
                    ? <Activity><SpinnerGap size={12} weight="bold" /> {phase === "cancelling" ? "Stopping…" : "Nexo is working…"}</Activity>
                    : <ItemMeta>{conversation.selectedModel ?? "Model not selected"}</ItemMeta>}
                </Open>
                <Archive
                  type="button"
                  aria-label={`Archive ${conversation.title}`}
                  onClick={(): void => onArchive(conversation)}
                >
                  <ArchiveIcon size={16} />
                </Archive>
              </Item>
              );
            })}
          </List>
        )}
      </Sidebar>
    </Fragment>
  );
}
