import { useEffect, useRef, type ReactElement } from "react";
import { Loading } from "../../../../../shared/components/Loading";
import type { ConversationMessage, StreamPhase } from "../../types/chatTypes";
import { MessageItem } from "./components/MessageItem";
import { Empty, EmptyTitle, Messages, StreamError } from "./styles";

type MessageListProps = {
  messages: ConversationMessage[];
  isLoading: boolean;
  hasConversation: boolean;
  hasModel: boolean;
  phase: StreamPhase;
  streamingContent: string;
  errorMessage: string | null;
};

export function MessageList({
  messages,
  isLoading,
  hasConversation,
  hasModel,
  phase,
  streamingContent,
  errorMessage
}: MessageListProps): ReactElement {
  const bottom = useRef<HTMLDivElement>(null);

  useEffect((): void => {
    bottom.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, streamingContent]);

  if (!hasConversation) {
    return (
      <Messages>
        <Empty>
          <EmptyTitle>No conversation open</EmptyTitle>
          Start a new chat to talk to a model running on your own machine.
        </Empty>
      </Messages>
    );
  }

  if (isLoading) return <Loading label="Opening this conversation…" />;

  const streamingId: string | null = phase === "streaming" || phase === "cancelling"
    ? messages.findLast((message: ConversationMessage) => message.role === "ASSISTANT")?.id ?? null
    : null;

  return (
    <Messages>
      {messages.length === 0 && (
        <Empty>
          <EmptyTitle>{hasModel ? "Ready when you are" : "Choose a model first"}</EmptyTitle>
          {hasModel
            ? "Write the first message and Nexo IA will answer with the selected model."
            : "Pick one of your configured providers above to start this conversation."}
        </Empty>
      )}

      {messages.map((message: ConversationMessage) => (
        <MessageItem
          key={message.id}
          message={message}
          streamingContent={streamingContent}
          isStreaming={message.id === streamingId}
        />
      ))}

      {errorMessage && <StreamError role="alert">{errorMessage}</StreamError>}
      {phase === "disconnected" && (
        <StreamError role="alert">
          The connection dropped. Reopen this conversation to see what was saved.
        </StreamError>
      )}

      <div ref={bottom} />
    </Messages>
  );
}
