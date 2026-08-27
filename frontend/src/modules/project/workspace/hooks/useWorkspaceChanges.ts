import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  approveWorkspaceChange,
  denyWorkspaceChange,
  listWorkspaceChanges,
  revertWorkspaceChange
} from "../api/workspaceChangeApi";

export const workspaceChangesKey = (conversationId: string | null) =>
  ["workspace-changes", conversationId] as const;

export const useWorkspaceChanges = (conversationId: string | null, enabled: boolean) => {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: workspaceChangesKey(conversationId),
    queryFn: (): Promise<Awaited<ReturnType<typeof listWorkspaceChanges>>> =>
      listWorkspaceChanges(conversationId as string),
    enabled: Boolean(conversationId) && enabled,
    refetchInterval: enabled ? 3_000 : false
  });
  const invalidate = (): Promise<void> => queryClient.invalidateQueries({
    queryKey: workspaceChangesKey(conversationId)
  });
  const approve = useMutation({ mutationFn: approveWorkspaceChange, onSuccess: invalidate });
  const deny = useMutation({ mutationFn: denyWorkspaceChange, onSuccess: invalidate });
  const revert = useMutation({ mutationFn: revertWorkspaceChange, onSuccess: invalidate });
  return { query, approve, deny, revert };
};
