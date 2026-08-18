import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import {
  archiveConversation,
  createConversation,
  listConversations,
  listMessages,
  renameConversation,
  selectConversationModel
} from "../api/chatApi";
import type { Conversation, ConversationMessage } from "../types/chatTypes";

export const conversationsKey = ["conversations"] as const;

export const messagesKey = (conversationId: string | null): readonly unknown[] =>
  ["conversations", conversationId, "messages"];

export const useConversations = (): UseQueryResult<Conversation[]> =>
  useQuery({ queryKey: conversationsKey, queryFn: listConversations });

export const useConversationMessages = (
  conversationId: string | null
): UseQueryResult<ConversationMessage[]> =>
  useQuery({
    queryKey: messagesKey(conversationId),
    queryFn: (): Promise<ConversationMessage[]> => listMessages(conversationId ?? ""),
    enabled: conversationId !== null
  });

export const useCreateConversation = (): UseMutationResult<Conversation, Error, string> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createConversation,
    onSuccess: (): Promise<void> => queryClient.invalidateQueries({ queryKey: conversationsKey })
  });
};

export const useRenameConversation = (): UseMutationResult<
  Conversation, Error, { conversationId: string; title: string }
> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ conversationId, title }: { conversationId: string; title: string }):
      Promise<Conversation> => renameConversation(conversationId, title),
    onSuccess: (): Promise<void> => queryClient.invalidateQueries({ queryKey: conversationsKey })
  });
};

export const useArchiveConversation = (): UseMutationResult<void, Error, string> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: archiveConversation,
    onSuccess: (): Promise<void> => queryClient.invalidateQueries({ queryKey: conversationsKey })
  });
};

export const useSelectConversationModel = (
  conversationId: string | null
): UseMutationResult<Conversation, Error, { providerConfigurationId: string; selectedModel: string }> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ providerConfigurationId, selectedModel }:
      { providerConfigurationId: string; selectedModel: string }): Promise<Conversation> =>
      selectConversationModel(conversationId ?? "", providerConfigurationId, selectedModel),
    onSuccess: (): Promise<void> => queryClient.invalidateQueries({ queryKey: conversationsKey })
  });
};
