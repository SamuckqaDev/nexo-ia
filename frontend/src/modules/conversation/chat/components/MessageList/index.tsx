import { Brain, ChatCircleDots, Cpu, ShieldCheck, Sparkle } from "@phosphor-icons/react";
import { useEffect, useRef, type ReactElement } from "react";
import type { ConversationMessage, ConversationMode, StreamPhase } from "../../types/chatTypes";
import { ChatLoading } from "../ChatLoading";
import { MessageItem } from "./components/MessageItem";
import { Empty, EmptyIcon, EmptyKicker, EmptyTitle, Feature, FeatureGrid, Messages, StreamError } from "./styles";

type MessageListProps = {
  messages: ConversationMessage[];
  isLoading: boolean;
  hasConversation: boolean;
  hasModel: boolean;
  phase: StreamPhase;
  streamingContent: string;
  errorMessage: string | null;
  mode: ConversationMode;
};

export function MessageList({
  messages,
  isLoading,
  hasConversation,
  hasModel,
  phase,
  streamingContent,
  errorMessage,
  mode
}: MessageListProps): ReactElement {
  const bottom = useRef<HTMLDivElement>(null);

  useEffect((): void => {
    bottom.current?.scrollIntoView?.({ behavior: "smooth", block: "end" });
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

  if (isLoading) return <ChatLoading title="Opening conversation" label="Nexo is restoring this private thread…" />;

  const streamingId: string | null = phase === "streaming" || phase === "cancelling"
    ? messages.findLast((message: ConversationMessage) => message.role === "ASSISTANT")?.id ?? null
    : null;
  const isThinking: boolean = phase === "starting"
    || ((phase === "streaming" || phase === "cancelling") && !streamingContent);
  const thinkingLabel: string = phase === "starting"
    ? "Reading your request and preparing the selected model…"
    : phase === "cancelling"
      ? "Stopping the response safely…"
      : "The model is preparing its first words…";

  return (
    <Messages>
      {messages.length === 0 && (
        <Empty>
          <EmptyIcon $agent={mode === "agent"}>{mode === "agent" ? <Brain size={28} weight="duotone" /> : <Sparkle size={28} weight="duotone" />}</EmptyIcon>
          <EmptyKicker>{mode === "agent" ? "Agent workspace" : "Nexo intelligence"}</EmptyKicker>
          <EmptyTitle>{hasModel ? mode === "agent" ? "Turn an objective into verified work" : "What are we exploring today?" : "Choose a model to begin"}</EmptyTitle>
          <span>{hasModel
            ? mode === "agent" ? "Your conversation stays here while Nexo prepares plans, permissions and evidence." : "Ask, create, analyse or bring an idea. Tools stay available when the task needs them."
            : "Pick one of your configured local models in the conversation header."}</span>
          {hasModel && <FeatureGrid>
            <Feature><ChatCircleDots size={17} weight="duotone" /><strong>Natural conversation</strong><small>Context stays inside this private thread.</small></Feature>
            <Feature><Cpu size={17} weight="duotone" /><strong>Local model</strong><small>Your selected provider handles the response.</small></Feature>
            <Feature><ShieldCheck size={17} weight="duotone" /><strong>Governed action</strong><small>Capabilities remain visible and controlled.</small></Feature>
          </FeatureGrid>}
        </Empty>
      )}

      {messages.map((message: ConversationMessage) => message.id === streamingId && isThinking
        ? null
        : (
          <MessageItem
            key={message.id}
            message={message}
            streamingContent={streamingContent}
            isStreaming={message.id === streamingId}
          />
        ))}

      {isThinking && <ChatLoading title="Thinking" label={thinkingLabel} variant="message" />}

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
