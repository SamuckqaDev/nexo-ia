export type ChatDraftState = {
  content: string;
  setContent: (content: string) => void;
  clear: () => void;
};
