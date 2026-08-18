export type AppLanguage = "en" | "pt-BR";

export type PreferenceState = {
  language: AppLanguage;
  setLanguage: (language: AppLanguage) => void;
};
