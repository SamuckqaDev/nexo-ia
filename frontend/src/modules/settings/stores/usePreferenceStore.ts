import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { PreferenceState } from "../types/preferenceTypes";

export const usePreferenceStore = create<PreferenceState>()(persist((set) => ({
  language: "en",
  setLanguage: (language): void => {
    set({ language });
  }
}), { name: "nexo-preferences" }));
