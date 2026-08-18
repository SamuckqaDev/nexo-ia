import * as DialogPrimitive from "@radix-ui/react-dialog";
import { useState, type FormEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { conversationTitleSchema } from "../../schemas/chatSchemas";
import { Actions, Backdrop, Dialog, Form, Message, Title } from "./styles";

type NewConversationDialogProps = {
  open: boolean;
  isSaving: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (title: string) => void;
};

export function NewConversationDialog({
  open,
  isSaving,
  onOpenChange,
  onCreate
}: NewConversationDialogProps): ReactElement {
  const [title, setTitle] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  const submit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    const parsed = conversationTitleSchema.safeParse(title);

    if (!parsed.success) {
      setError(parsed.error.issues[0]?.message ?? "Name this conversation");
      return;
    }

    setError(null);
    setTitle("");
    onCreate(parsed.data);
  };

  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <Backdrop />
        <Dialog>
          <Title>New conversation</Title>
          <Message>Give it a name so you can find it again later.</Message>

          <Form onSubmit={submit}>
            <Input
              id="new-conversation-title"
              label="Conversation name"
              value={title}
              error={error ?? undefined}
              autoFocus
              onChange={(event): void => setTitle(event.target.value)}
            />
            <Actions>
              <DialogPrimitive.Close asChild>
                <Button type="button" variant="outline">Cancel</Button>
              </DialogPrimitive.Close>
              <Button type="submit" disabled={isSaving}>
                {isSaving ? "Creating…" : "Create"}
              </Button>
            </Actions>
          </Form>
        </Dialog>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
