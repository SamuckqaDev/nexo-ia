import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createConversation, listConversations, listMessages, selectConversationModel, sendMessage } from "../api/chatApi";
import type { Conversation } from "../types/chatTypes";

export const useConversations = () => useQuery({ queryKey: ["conversations"], queryFn: listConversations });
export const useConversationMessages = (conversationId: string | null) => useQuery({ queryKey: ["conversations", conversationId, "messages"], queryFn: () => listMessages(conversationId ?? ""), enabled: conversationId !== null });
export const useCreateConversation = () => {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: createConversation, onSuccess: (conversation: Conversation) => queryClient.setQueryData<Conversation[]>(["conversations"], (items: Conversation[] | undefined) => [conversation, ...(items ?? [])]) });
};
export const useSendMessage = (conversationId: string | null) => {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (content: string) => sendMessage(conversationId ?? "", content), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["conversations", conversationId, "messages"] }) });
};
export const useSelectConversationModel = (conversationId: string | null) => {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ providerConfigurationId, selectedModel }: { providerConfigurationId: string; selectedModel: string }) => selectConversationModel(conversationId ?? "", providerConfigurationId, selectedModel), onSuccess: (conversation: Conversation) => queryClient.setQueryData<Conversation[]>(["conversations"], (items: Conversation[] | undefined) => items?.map((item) => item.id === conversation.id ? conversation : item) ?? []) });
};
