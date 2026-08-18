import { Check, Copy, Prohibit, Sparkle, UserCircle, WarningCircle } from "@phosphor-icons/react";
import { useEffect, useState, type ReactElement } from "react";
import type { ConversationMessage } from "../../../../types/chatTypes";
import { ActionBar, Actor, Badge, Body, Bubble, Caret, Content, CopyButton, Meta, Role } from "./styles";

type MessageItemProps = {
  message: ConversationMessage;
  streamingContent?: string;
  isStreaming?: boolean;
};

/**
 * Renders one persisted message. A cancelled or failed generation always states what happened, so a
 * partial or missing answer is never presented as a normal reply.
 */
export function MessageItem({
  message,
  streamingContent,
  isStreaming = false
}: MessageItemProps): ReactElement {
  const isUser: boolean = message.role === "USER";
  const content: string = isStreaming ? streamingContent ?? "" : message.content;
  const [copied, setCopied] = useState<boolean>(false);

  useEffect((): void => setCopied(false), [content]);

  const copyContent = (): void => {
    if (!content || !navigator.clipboard) return;
    navigator.clipboard.writeText(content)
      .then((): void => setCopied(true))
      .catch((): void => undefined);
  };

  return (
    <Bubble $user={isUser} aria-live={isStreaming ? "polite" : undefined}>
      <Actor $user={isUser}>{isUser ? <UserCircle size={19} weight="fill" /> : <Sparkle size={18} weight="fill" />}</Actor>
      <Content $user={isUser}>
        <ActionBar $user={isUser}>
          <Role $user={isUser}>{isUser ? "You" : "Nexo IA"}</Role>
          <CopyButton type="button" onClick={copyContent} disabled={!content} aria-label={copied ? "Content copied" : "Copy content"}>
            {copied ? <Check size={14} weight="bold" /> : <Copy size={14} />}
            <span>{copied ? "Copied" : "Copy"}</span>
          </CopyButton>
        </ActionBar>
        <Body>{content}{isStreaming && <Caret aria-hidden>▌</Caret>}</Body>

        {message.status === "CANCELLED" && (
          <Badge $tone="warning"><Prohibit size={14} weight="bold" aria-hidden />You stopped this answer{message.content ? " while it was being written" : " before it started"}</Badge>
        )}

        {message.status === "FAILED" && (
          <Badge $tone="danger"><WarningCircle size={14} weight="bold" aria-hidden />This answer failed and was not completed</Badge>
        )}

        {!isUser && message.status === "COMPLETED" && message.model && (
          <Meta><span>{message.model}</span>{message.outputTokens !== null && <span>{message.inputTokens ?? 0} in · {message.outputTokens} out{message.tokenSource === "ESTIMATE" ? " (estimated)" : ""}</span>}{message.latencyMs !== null && <span>{(message.latencyMs / 1000).toFixed(1)}s</span>}{message.processingLocation && <span>{message.processingLocation.toLowerCase()}</span>}</Meta>
        )}
      </Content>
    </Bubble>
  );
}
