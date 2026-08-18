import { create } from "zustand";
import type { ConfirmationRequest, ConfirmationState } from "../types/confirmationTypes";

export const useConfirmationStore = create<ConfirmationState>((set, get) => ({
  request: null,
  resolver: null,
  ask: (request: ConfirmationRequest): Promise<boolean> => new Promise<boolean>((resolve) => {
    set({ request, resolver: resolve });
  }),
  answer: (confirmed: boolean): void => {
    const resolver: ((confirmed: boolean) => void) | null = get().resolver;
    set({ request: null, resolver: null });
    resolver?.(confirmed);
  }
}));
