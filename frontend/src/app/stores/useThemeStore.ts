import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { ThemeState } from "../types/themeTypes";

const preferredMode = (): "dark" | "light" =>
  window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";

export const useThemeStore = create<ThemeState>()(persist((set) => ({
  mode: preferredMode(),
  toggle: (): void => {
    set((state: ThemeState) => ({ mode: state.mode === "dark" ? "light" : "dark" }));
  },
  setMode: (mode): void => {
    set({ mode });
  }
}), { name: "nexo-visual-theme" }));
