import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { PreferenceState } from "../types/preferenceTypes";

export const usePreferenceStore = create<PreferenceState>()(persist((set) => ({
  language: "en",
  thinkingEnabled: false,
  setLanguage: (language): void => {
    set({ language });
  },
  setThinkingEnabled: (thinkingEnabled): void => {
    set({ thinkingEnabled });
  }
}), { name: "nexo-preferences" }));
