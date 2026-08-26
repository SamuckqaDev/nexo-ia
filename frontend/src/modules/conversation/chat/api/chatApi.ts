import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { conversationMessageSchema, conversationSchema } from "../schemas/chatSchemas";
import type { Conversation, ConversationMessage } from "../types/chatTypes";

const first = <T>(response: BaseResponse<unknown>, parse: (value: unknown) => T): T => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty response");
  return parse(value);
};

export const listConversations = (): Promise<Conversation[]> =>
  apiClient.get<BaseResponse<unknown>>("/conversations")
    .then(({ data }) => (data.data ?? []).map((item: unknown) => conversationSchema.parse(item)));

export const createConversation = (title: string): Promise<Conversation> =>
  apiClient.post<BaseResponse<unknown>>("/conversations", { title })
    .then(({ data }) => first(data, (value) => conversationSchema.parse(value)));

export const renameConversation = (conversationId: string, title: string): Promise<Conversation> =>
  apiClient.put<BaseResponse<unknown>>(`/conversations/${conversationId}`, { title })
    .then(({ data }) => first(data, (value) => conversationSchema.parse(value)));

export const archiveConversation = (conversationId: string): Promise<void> =>
  apiClient.delete<BaseResponse<unknown>>(`/conversations/${conversationId}`).then(() => undefined);

export const listMessages = (conversationId: string): Promise<ConversationMessage[]> =>
  apiClient.get<BaseResponse<unknown>>(`/conversations/${conversationId}/messages`)
    .then(({ data }) => (data.data ?? [])
      .map((item: unknown) => conversationMessageSchema.parse(item)));

export const selectConversationModel = (
  conversationId: string,
  providerConfigurationId: string,
  selectedModel: string
): Promise<Conversation> =>
  apiClient.put<BaseResponse<unknown>>(`/conversations/${conversationId}/model`, {
    providerConfigurationId,
    selectedModel
  }).then(({ data }) => first(data, (value) => conversationSchema.parse(value)));

export const selectConversationKnowledge = (
  conversationId: string,
  vaultIds: string[]
): Promise<Conversation> =>
  apiClient.put<BaseResponse<unknown>>(`/conversations/${conversationId}/knowledge-vaults`, {
    vaultIds
  }).then(({ data }) => first(data, (value) => conversationSchema.parse(value)));

export const selectConversationWorkspace = (
  conversationId: string,
  workspaceId: string | null
): Promise<Conversation> =>
  apiClient.put<BaseResponse<unknown>>(`/conversations/${conversationId}/workspace`, {
    workspaceId
  }).then(({ data }) => first(data, (value) => conversationSchema.parse(value)));

export const cancelModelRequest = (conversationId: string, messageId: string): Promise<void> =>
  apiClient.post<BaseResponse<unknown>>(
    `/conversations/${conversationId}/messages/${messageId}/cancel`
  ).then(() => undefined);
