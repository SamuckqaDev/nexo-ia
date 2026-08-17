import { create } from "zustand";
import type { SnackbarMessage, SnackbarOptions, SnackbarState } from "../types/snackbarTypes";

export const useSnackbarStore = create<SnackbarState>((set) => ({
  messages: [],
  show: (message: string, options?: SnackbarOptions): void => set((state: SnackbarState) => ({
    messages: [
      ...state.messages,
      {
        id: crypto.randomUUID(),
        message,
        variant: options?.variant ?? "info",
        duration: options?.duration ?? 5000
      }
    ]
  })),
  dismiss: (id: string): void => set((state: SnackbarState) => ({
    messages: state.messages.filter((message: SnackbarMessage): boolean => message.id !== id)
  }))
}));
