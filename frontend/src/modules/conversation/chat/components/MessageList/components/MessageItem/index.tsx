import { Brain, Check, Copy, Prohibit, Quotes, Sparkle, UserCircle, WarningCircle } from "@phosphor-icons/react";
import { useEffect, useState, type ReactElement } from "react";
import { parseContextualChatMessage } from "../../../../services/chatContextService";
import type { ConversationMessage } from "../../../../types/chatTypes";
import { MessageContent } from "./components/MessageContent";
import {
  Avatar,
  Badge,
  Column,
  ContextBadge,
  ContextBadges,
  ContextMeter,
  ContextMeterTrack,
  CopyButton,
  Head,
  Meta,
  Name,
  Row,
  ThinkingTrace
} from "./styles";

type MessageItemProps = {
  message: ConversationMessage;
  streamingContent?: string;
  isStreaming?: boolean;
  thinkingContent?: string;
};

/**
 * Renders one full-width conversation turn. A cancelled or failed generation always states what
 * happened, so a partial or missing answer is never presented as a normal reply.
 */
export function MessageItem({
  message,
  streamingContent,
  isStreaming = false,
  thinkingContent = ""
}: MessageItemProps): ReactElement {
  const isUser: boolean = message.role === "USER";
  const rawContent: string = isStreaming ? streamingContent ?? "" : message.content;
  const [displayedContent, setDisplayedContent] = useState<string>(rawContent);

  useEffect((): (() => void) | void => {
    if (!isStreaming) {
      setDisplayedContent(rawContent);
      return undefined;
    }

    if (displayedContent.length >= rawContent.length) return undefined;

    const timer: number = window.setInterval((): void => {
      setDisplayedContent((current: string): string => {
        if (current.length >= rawContent.length) return current;
        return rawContent.slice(0, Math.min(rawContent.length, current.length + 2));
      });
    }, 14);

    return (): void => window.clearInterval(timer);
  }, [displayedContent.length, isStreaming, rawContent]);

  const contextualMessage = isUser
    ? parseContextualChatMessage(displayedContent)
    : { content: displayedContent, skillName: null, vaultSourceNames: [] };
  const content: string = contextualMessage.content;
  const contextPercentage: number | null = message.contextTokensUsed !== null && message.contextTokenBudget
    ? Math.min(100, (message.contextTokensUsed / message.contextTokenBudget) * 100)
    : null;
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

        {!isUser && isStreaming && (
          <ThinkingTrace>
            <summary><Brain size={14} weight="duotone" /> Thinking</summary>
            <p>{thinkingContent || "Nexo is thinking through the request…"}</p>
          </ThinkingTrace>
        )}

        <MessageContent content={content} isStreaming={isStreaming} isUser={isUser} />

        {isUser && (contextualMessage.skillName || contextualMessage.vaultSourceNames.length > 0) && (
          <ContextBadges>
            {contextualMessage.skillName && <ContextBadge><Sparkle size={13} weight="fill" /> Skill: {contextualMessage.skillName}</ContextBadge>}
            {contextualMessage.vaultSourceNames.map((sourceName: string) => <ContextBadge key={sourceName}>Vault: {sourceName}</ContextBadge>)}
          </ContextBadges>
        )}

        {!isUser && message.status === "COMPLETED" && message.citations && message.citations.length > 0 && (
          <ContextBadges aria-label="Knowledge Vault citations">
            {message.citations.map((citation, index) => (
              <ContextBadge
                key={`${citation.vaultName}-${citation.sourceDisplayName}-${citation.chunkOrdinal}-${index}`}
                title={citation.excerpt}
              >
                <Quotes size={13} weight="fill" /> {citation.vaultName}/{citation.sourceDisplayName}#{citation.chunkOrdinal}
              </ContextBadge>
            ))}
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
            {message.totalTokens !== null && <span><strong>{message.totalTokens.toLocaleString()}</strong> total tokens</span>}
            {message.latencyMs !== null && <span>Response time {(message.latencyMs / 1000).toFixed(1)}s</span>}
            {message.processingLocation && <span>{message.processingLocation.toLowerCase()}</span>}
            {contextPercentage !== null && message.contextTokensUsed !== null && message.contextTokenBudget !== null && (
              <ContextMeter title="Nexo context assembly budget, not the model's discovered maximum window">
                <span>Context {message.contextTokensUsed.toLocaleString()} / {message.contextTokenBudget.toLocaleString()} ({contextPercentage.toFixed(1)}%)</span>
                <ContextMeterTrack $percentage={contextPercentage}><i /></ContextMeterTrack>
              </ContextMeter>
            )}
          </Meta>
        )}
      </Column>
    </Row>
  );
}
