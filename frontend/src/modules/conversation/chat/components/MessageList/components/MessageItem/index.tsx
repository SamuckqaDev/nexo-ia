import { Check, Copy, Prohibit, Sparkle, UserCircle, WarningCircle } from "@phosphor-icons/react";
import { useEffect, useState, type ReactElement } from "react";
import { parseContextualChatMessage } from "../../../../services/chatContextService";
import type { ConversationMessage } from "../../../../types/chatTypes";
import { Avatar, Badge, Body, Caret, Column, ContextBadge, ContextBadges, CopyButton, Head, Meta, Name, Row } from "./styles";

type MessageItemProps = {
  message: ConversationMessage;
  streamingContent?: string;
  isStreaming?: boolean;
};

/**
 * Renders one full-width conversation turn. A cancelled or failed generation always states what
 * happened, so a partial or missing answer is never presented as a normal reply.
 */
export function MessageItem({
  message,
  streamingContent,
  isStreaming = false
}: MessageItemProps): ReactElement {
  const isUser: boolean = message.role === "USER";
  const rawContent: string = isStreaming ? streamingContent ?? "" : message.content;
  const contextualMessage = isUser ? parseContextualChatMessage(rawContent) : { content: rawContent, skillName: null, vaultSourceNames: [] };
  const content: string = contextualMessage.content;
  const [copied, setCopied] = useState<boolean>(false);

  useEffect((): void => setCopied(false), [content]);

  const copyContent = (): void => {
    if (!content || !navigator.clipboard) return;
    navigator.clipboard.writeText(content)
      .then((): void => setCopied(true))
      .catch((): void => undefined);
  };

  return (
    <Row $user={isUser}>
      <Avatar $user={isUser}>
        {isUser ? <UserCircle size={18} weight="fill" /> : <Sparkle size={17} weight="fill" />}
      </Avatar>
      <Column>
        <Head>
          <Name $user={isUser}>{isUser ? "You" : "Nexo IA"}</Name>
          <CopyButton
            type="button"
            onClick={copyContent}
            disabled={!content}
            aria-label={copied ? "Content copied" : "Copy message"}
          >
            {copied ? <Check size={13} weight="bold" /> : <Copy size={13} />}
            <span>{copied ? "Copied" : "Copy"}</span>
          </CopyButton>
        </Head>

        <Body $user={isUser}>{content}{isStreaming && <Caret aria-hidden>▌</Caret>}</Body>

        {isUser && (contextualMessage.skillName || contextualMessage.vaultSourceNames.length > 0) && (
          <ContextBadges>
            {contextualMessage.skillName && <ContextBadge><Sparkle size={13} weight="fill" /> Skill: {contextualMessage.skillName}</ContextBadge>}
            {contextualMessage.vaultSourceNames.map((sourceName: string) => <ContextBadge key={sourceName}>Vault: {sourceName}</ContextBadge>)}
          </ContextBadges>
        )}

        {message.status === "CANCELLED" && (
          <Badge $tone="warning">
            <Prohibit size={14} weight="bold" aria-hidden />
            You stopped this answer{message.content ? " while it was being written" : " before it started"}
          </Badge>
        )}

        {message.status === "FAILED" && (
          <Badge $tone="danger">
            <WarningCircle size={14} weight="bold" aria-hidden />
            This answer failed and was not completed
          </Badge>
        )}

        {!isUser && message.status === "COMPLETED" && message.model && (
          <Meta>
            <span>{message.model}</span>
            {message.outputTokens !== null && (
              <span>
                {message.inputTokens ?? 0} in · {message.outputTokens} out
                {message.tokenSource === "ESTIMATE" ? " (estimated)" : ""}
              </span>
            )}
            {message.latencyMs !== null && <span>{(message.latencyMs / 1000).toFixed(1)}s</span>}
            {message.processingLocation && <span>{message.processingLocation.toLowerCase()}</span>}
          </Meta>
        )}
      </Column>
    </Row>
  );
}
