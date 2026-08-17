import { useEffect, type ReactElement } from "react";
import { CheckCircle, Info, Warning, WarningCircle, X } from "@phosphor-icons/react";
import { useSnackbarStore } from "../../stores/useSnackbarStore";
import type { SnackbarIconMap, SnackbarMessage, SnackbarState } from "../../types/snackbarTypes";
import { Close, Message, Region, Text } from "./styles";

const icons: SnackbarIconMap = {
  success: CheckCircle,
  error: WarningCircle,
  info: Info,
  warning: Warning
};

export function Snackbar(): ReactElement | null {
  const message: SnackbarMessage | undefined = useSnackbarStore(
    (state: SnackbarState): SnackbarMessage | undefined => state.messages[0]
  );
  const dismiss: SnackbarState["dismiss"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["dismiss"] => state.dismiss
  );

  useEffect(() => {
    if (!message || message.duration === 0) return;
    const timeout: number = window.setTimeout((): void => dismiss(message.id), message.duration);
    return () => window.clearTimeout(timeout);
  }, [dismiss, message]);

  if (!message) return null;

  const StatusIcon: SnackbarIconMap[SnackbarMessage["variant"]] = icons[message.variant];

  return (
    <Region aria-live={message.variant === "error" ? "assertive" : "polite"}>
      <Message role={message.variant === "error" ? "alert" : "status"} $variant={message.variant}>
        <StatusIcon aria-hidden size={24} weight="fill" />
        <Text>{message.message}</Text>
        <Close type="button" aria-label="Dismiss notification" onClick={() => dismiss(message.id)}>
          <X aria-hidden size={18} weight="bold" />
        </Close>
      </Message>
    </Region>
  );
}
