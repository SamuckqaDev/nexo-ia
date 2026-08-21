import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { createKnowledgeWorkspace, listKnowledgeWorkspaces } from "../api/knowledgeWorkspaceApi";
import type { KnowledgeWorkspace } from "../types/knowledgeWorkspaceTypes";

export const knowledgeWorkspacesKey = ["knowledge", "workspaces"] as const;

export function useKnowledgeWorkspaces(): {
  workspaces: UseQueryResult<KnowledgeWorkspace[]>;
  create: UseMutationResult<KnowledgeWorkspace, Error, string>;
} {
  const queryClient: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);

  const workspaces = useQuery({ queryKey: knowledgeWorkspacesKey, queryFn: listKnowledgeWorkspaces, retry: false });
  const create = useMutation({
    mutationFn: createKnowledgeWorkspace,
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: knowledgeWorkspacesKey });
      show("Workspace created.", { variant: "success" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });

  return { workspaces, create };
}
