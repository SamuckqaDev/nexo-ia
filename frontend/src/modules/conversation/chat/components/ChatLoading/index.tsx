import type { ReactElement } from "react";
import type { ChatLoadingProps } from "../../types/chatViewTypes";
import { Copy, Dots, Face, Loader, Pulse } from "./styles";

export function ChatLoading({
  title,
  label
}: ChatLoadingProps): ReactElement {
  return (
    <Loader role="status" aria-live="polite">
      <Pulse><Face src="/assets/logo/nexo-ia-symbol.png" alt="" /></Pulse>
      <Copy>
        <strong>{title}</strong>
        <span>{label}</span>
      </Copy>
      <Dots aria-hidden><i /><i /><i /></Dots>
    </Loader>
  );
}
