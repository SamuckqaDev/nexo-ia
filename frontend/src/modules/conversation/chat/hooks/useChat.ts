import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import {
  archiveConversation,
  createConversation,
  listConversations,
  listMessages,
  renameConversation,
  selectConversationKnowledge,
  selectConversationModel,
  selectConversationWorkspace
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
): UseMutationResult<
  Conversation,
  Error,
  { providerConfigurationId: string; selectedModel: string },
  { previous: Conversation[] | undefined }
> => {
  const queryClient = useQueryClient();

  return useMutation<
    Conversation,
    Error,
    { providerConfigurationId: string; selectedModel: string },
    { previous: Conversation[] | undefined }
  >({
    mutationFn: ({ providerConfigurationId, selectedModel }:
      { providerConfigurationId: string; selectedModel: string }): Promise<Conversation> =>
      selectConversationModel(conversationId ?? "", providerConfigurationId, selectedModel),
    onMutate: ({ providerConfigurationId, selectedModel }): Promise<{ previous: Conversation[] | undefined }> =>
      queryClient.cancelQueries({ queryKey: conversationsKey }).then(() => {
        const previous: Conversation[] | undefined = queryClient.getQueryData<Conversation[]>(conversationsKey);
        queryClient.setQueryData<Conversation[]>(conversationsKey, (current: Conversation[] | undefined): Conversation[] =>
          (current ?? []).map((conversation: Conversation): Conversation => conversation.id === conversationId
            ? { ...conversation, providerConfigurationId, selectedModel }
            : conversation));
        return { previous };
      }),
    onError: (_error, _variables, context): void => {
      if (context?.previous) queryClient.setQueryData(conversationsKey, context.previous);
    },
    onSettled: (): Promise<void> => queryClient.invalidateQueries({ queryKey: conversationsKey })
  });
};

export const useSelectConversationKnowledge = (
  conversationId: string | null
): UseMutationResult<
  Conversation,
  Error,
  string[],
  { previous: Conversation[] | undefined }
> => {
  const queryClient = useQueryClient();

  return useMutation<
    Conversation,
    Error,
    string[],
    { previous: Conversation[] | undefined }
  >({
    mutationFn: (vaultIds: string[]): Promise<Conversation> =>
      selectConversationKnowledge(conversationId ?? "", vaultIds),
    onMutate: (vaultIds: string[]): Promise<{ previous: Conversation[] | undefined }> =>
      queryClient.cancelQueries({ queryKey: conversationsKey }).then(() => {
        const previous: Conversation[] | undefined =
          queryClient.getQueryData<Conversation[]>(conversationsKey);
        queryClient.setQueryData<Conversation[]>(
          conversationsKey,
          (current: Conversation[] | undefined): Conversation[] =>
            (current ?? []).map((conversation: Conversation): Conversation =>
              conversation.id === conversationId
                ? { ...conversation, knowledgeVaultIds: vaultIds }
                : conversation)
        );
        return { previous };
      }),
    onError: (_error, _variables, context): void => {
      if (context?.previous) queryClient.setQueryData(conversationsKey, context.previous);
    },
    onSettled: (): Promise<void> =>
      queryClient.invalidateQueries({ queryKey: conversationsKey })
  });
};

export const useSelectConversationWorkspace = (
  conversationId: string | null
): UseMutationResult<
  Conversation,
  Error,
  string | null,
  { previous: Conversation[] | undefined }
> => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (workspaceId: string | null): Promise<Conversation> =>
      selectConversationWorkspace(conversationId ?? "", workspaceId),
    onMutate: (workspaceId: string | null): Promise<{ previous: Conversation[] | undefined }> =>
      queryClient.cancelQueries({ queryKey: conversationsKey }).then(() => {
        const previous = queryClient.getQueryData<Conversation[]>(conversationsKey);
        queryClient.setQueryData<Conversation[]>(conversationsKey, (current) =>
          (current ?? []).map((conversation) => conversation.id === conversationId
            ? { ...conversation, workspaceId }
            : conversation));
        return { previous };
      }),
    onError: (_error, _variables, context): void => {
      if (context?.previous) queryClient.setQueryData(conversationsKey, context.previous);
    },
    onSettled: (): Promise<void> => queryClient.invalidateQueries({ queryKey: conversationsKey })
  });
};
