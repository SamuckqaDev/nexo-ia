import { PaperPlaneRight, Stop } from "@phosphor-icons/react";
import { useState, type FormEvent, type KeyboardEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type { StreamPhase } from "../../types/chatTypes";
import { Composer, Field, Hint } from "./styles";

type ChatComposerProps = {
  disabled: boolean;
  hasModel: boolean;
  phase: StreamPhase;
  isBusy: boolean;
  onSend: (content: string) => void;
  onCancel: () => void;
};

export function ChatComposer({
  disabled,
  hasModel,
  phase,
  isBusy,
  onSend,
  onCancel
}: ChatComposerProps): ReactElement {
  const [content, setContent] = useState<string>("");

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
    <div>
      {!hasModel && !disabled && <Hint>Select a model to enable this conversation.</Hint>}
      {phase === "cancelling" && <Hint>Stopping the answer…</Hint>}

      <Composer onSubmit={submit}>
        <Field
          aria-label="Message"
          placeholder={hasModel ? "Message Nexo IA…" : "Choose a model first"}
          value={content}
          disabled={disabled || !hasModel || isBusy}
          onChange={(event): void => setContent(event.target.value)}
          onKeyDown={submitOnEnter}
        />

        {isBusy ? (
          <Button
            type="button"
            variant="outline"
            icon={Stop}
            disabled={phase === "cancelling"}
            onClick={onCancel}
          >
            Stop
          </Button>
        ) : (
          <Button type="submit" icon={PaperPlaneRight} disabled={disabled || !hasModel || !content.trim()}>
            Send
          </Button>
        )}
      </Composer>
    </div>
  );
}
