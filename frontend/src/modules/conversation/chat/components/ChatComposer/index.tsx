import { ChatCircleDots, ImageSquare, PaperPlaneRight, Robot, Stop, Wrench } from "@phosphor-icons/react";
import { useState, type FormEvent, type KeyboardEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type { ConversationMode, StreamPhase } from "../../types/chatTypes";
import {
  CapabilityButton,
  Composer,
  ComposerCard,
  ComposerFooter,
  Field,
  Hint,
  ModeButton,
  ModeControl,
  ModeHint,
  SendButton
} from "./styles";

type ChatComposerProps = {
  initialContent?: string;
  disabled: boolean;
  hasModel: boolean;
  phase: StreamPhase;
  isBusy: boolean;
  mode: ConversationMode;
  onModeChange: (mode: ConversationMode) => void;
  onSend: (content: string) => void;
  onCancel: () => void;
};

export function ChatComposer({
  initialContent = "",
  disabled,
  hasModel,
  phase,
  isBusy,
  mode,
  onModeChange,
  onSend,
  onCancel
}: ChatComposerProps): ReactElement {
  const [content, setContent] = useState<string>(initialContent);

  const submit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    if (!content.trim() || isBusy) return;

    onSend(content.trim());
    setContent("");
  };

  const submitOnEnter = (event: KeyboardEvent<HTMLTextAreaElement>): void => {
    if (event.key !== "Enter" || event.shiftKey || isBusy || !content.trim()) return;

    event.preventDefault();
    onSend(content.trim());
    setContent("");
  };

  return (
    <ComposerCard>
      {!hasModel && !disabled && <Hint>Select a model to enable this conversation.</Hint>}
      {phase === "cancelling" && <Hint>Stopping the answer…</Hint>}
      {mode === "agent" && (
        <ModeHint>
          Agent keeps this conversation, but adds a visible plan, permissions and verified steps.
          Its execution runtime is not enabled yet.
        </ModeHint>
      )}

      <Composer onSubmit={submit}>
        <Field
          aria-label="Message"
          placeholder={hasModel
            ? mode === "agent" ? "Describe an objective for Nexo Agent…" : "Message Nexo IA…"
            : "Choose a model first"}
          value={content}
          disabled={disabled || !hasModel || isBusy || mode === "agent"}
          onChange={(event): void => setContent(event.target.value)}
          onKeyDown={submitOnEnter}
        />

        <ComposerFooter>
          <ModeControl aria-label="Conversation mode">
            <ModeButton type="button" $active={mode === "chat"} onClick={(): void => onModeChange("chat")}>
              <ChatCircleDots size={15} weight="duotone" /> Chat
            </ModeButton>
            <ModeButton type="button" $active={mode === "agent"} $agent onClick={(): void => onModeChange("agent")}>
              <Robot size={15} weight="duotone" /> Agent
            </ModeButton>
          </ModeControl>
          <CapabilityButton type="button" disabled title="Tools will appear here when enabled">
            <Wrench size={16} weight="duotone" /> Tools
          </CapabilityButton>
          <CapabilityButton type="button" disabled title="Image generation requires the ComfyUI runtime">
            <ImageSquare size={16} weight="duotone" /> Image
          </CapabilityButton>
          {isBusy ? (
            <Button type="button" variant="outline" icon={Stop} disabled={phase === "cancelling"} onClick={onCancel}>
              Stop
            </Button>
          ) : (
            <SendButton type="submit" aria-label="Send message" disabled={disabled || !hasModel || !content.trim() || mode === "agent"}>
              <PaperPlaneRight size={19} weight="fill" />
            </SendButton>
          )}
        </ComposerFooter>
      </Composer>
    </ComposerCard>
  );
}
