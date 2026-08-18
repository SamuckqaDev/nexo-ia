import { create } from "zustand";

export type SessionExpiredState = {
  isOpen: boolean;
  open: () => void;
  close: () => void;
};

export const useSessionExpiredStore = create<SessionExpiredState>((set) => ({
  isOpen: false,
  open: (): void => set({ isOpen: true }),
  close: (): void => set({ isOpen: false })
}));
