export type AppLanguage = "en" | "pt-BR";

export type PreferenceState = {
  language: AppLanguage;
  thinkingEnabled: boolean;
  setLanguage: (language: AppLanguage) => void;
  setThinkingEnabled: (enabled: boolean) => void;
};
