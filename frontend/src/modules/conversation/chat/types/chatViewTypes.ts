import type { ConversationMode } from "./chatTypes";

export type ConversationContextSection = "plan" | "artifacts" | "media" | "tasks";

export type ConversationContextPanelProps = {
  mode: ConversationMode;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export type ChatLoadingProps = {
  title: string;
  label: string;
};
