import { z } from "zod";
import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import type { Conversation, ConversationMessage } from "../types/chatTypes";

const conversationSchema = z.object({ id: z.uuid(), title: z.string(), providerConfigurationId: z.uuid().nullable(), selectedModel: z.string().nullable(), createdAt: z.iso.datetime(), updatedAt: z.iso.datetime() });
const messageSchema = z.object({ id: z.uuid(), role: z.enum(["USER", "ASSISTANT"]), content: z.string(), createdAt: z.iso.datetime() });
const first = <T>(response: BaseResponse<T>): T => {
  const value: T | undefined = response.data?.[0];
  if (!value) throw new Error("Nexo returned an empty response");
  return value;
};

export const listConversations = (): Promise<Conversation[]> => apiClient.get<BaseResponse<unknown>>("/conversations")
  .then(({ data }) => (data.data ?? []).map((item: unknown) => conversationSchema.parse(item)));
export const createConversation = (title: string): Promise<Conversation> => apiClient.post<BaseResponse<unknown>>("/conversations", { title })
  .then(({ data }) => conversationSchema.parse(first(data)));
export const listMessages = (conversationId: string): Promise<ConversationMessage[]> => apiClient.get<BaseResponse<unknown>>(`/conversations/${conversationId}/messages`)
  .then(({ data }) => (data.data ?? []).map((item: unknown) => messageSchema.parse(item)));
export const sendMessage = (conversationId: string, content: string): Promise<ConversationMessage> => apiClient.post<BaseResponse<unknown>>(`/conversations/${conversationId}/messages`, { content })
  .then(({ data }) => messageSchema.parse(first(data)));
export const selectConversationModel = (conversationId: string, providerConfigurationId: string, selectedModel: string): Promise<Conversation> => apiClient.put<BaseResponse<unknown>>(`/conversations/${conversationId}/model`, { providerConfigurationId, selectedModel })
  .then(({ data }) => conversationSchema.parse(first(data)));
