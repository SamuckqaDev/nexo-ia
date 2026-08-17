export type SnackbarVariant = "success" | "error" | "info" | "warning";

export type SnackbarMessage = {
  id: string;
  message: string;
  variant: SnackbarVariant;
  duration: number;
};

export type SnackbarOptions = {
  variant?: SnackbarVariant;
  duration?: number;
};

export type SnackbarState = {
  messages: SnackbarMessage[];
  show: (message: string, options?: SnackbarOptions) => void;
  dismiss: (id: string) => void;
};

export type SnackbarIconMap = Record<SnackbarVariant, Icon>;
import type { Icon } from "@phosphor-icons/react";
