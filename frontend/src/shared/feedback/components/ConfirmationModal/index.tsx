import { WarningDiamond, X } from "@phosphor-icons/react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import type { ReactElement } from "react";
import type { ConfirmationState } from "../../types/confirmationTypes";
import { useConfirmationStore } from "../../stores/useConfirmationStore";
import { Actions, Backdrop, CancelButton, CloseButton, ConfirmButton, Copy, Dialog, Icon, Message, Title } from "./styles";

export function ConfirmationModal(): ReactElement | null {
  const request: ConfirmationState["request"] = useConfirmationStore((state: ConfirmationState) => state.request);
  const answer: ConfirmationState["answer"] = useConfirmationStore((state: ConfirmationState) => state.answer);

  if (!request) return null;

  return (
    <DialogPrimitive.Root open onOpenChange={(open: boolean): void => { if (!open) answer(false); }}>
      <DialogPrimitive.Portal>
        <Backdrop />
        <Dialog role="alertdialog">
          <CloseButton aria-label="Close confirmation"><X size={18} /></CloseButton>
          <Icon $tone={request.tone}><WarningDiamond size={26} weight="duotone" /></Icon>
          <Copy>
            <Title>{request.title}</Title>
            <Message>{request.message}</Message>
          </Copy>
          <Actions>
            <DialogPrimitive.Close asChild>
              <CancelButton type="button">Cancel</CancelButton>
            </DialogPrimitive.Close>
            <ConfirmButton type="button" $tone={request.tone} onClick={(): void => answer(true)}>{request.confirmLabel}</ConfirmButton>
          </Actions>
        </Dialog>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
