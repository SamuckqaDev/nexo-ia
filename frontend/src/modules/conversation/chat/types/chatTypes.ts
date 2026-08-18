export type MessageRole = "USER" | "ASSISTANT";

export interface Conversation {
  id: string;
  title: string;
  providerConfigurationId: string | null;
  selectedModel: string | null;
  createdAt: string;
  updatedAt: string;
}

export type ConversationMode = "chat" | "agent";

export interface ConversationMessage {
  id: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}
