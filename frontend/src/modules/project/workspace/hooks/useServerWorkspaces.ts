import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import {
  createServerWorkspace,
  deleteServerWorkspace,
  getServerWorkspaceStatus,
  getServerWorkspaceTree,
  getLocalWorkspaceTree,
  listWorkspaceBindings,
  listServerWorkspaces,
  refreshServerWorkspace
} from "../api/serverWorkspaceApi";
import type {
  CreateServerWorkspaceInput,
  ServerWorkspace,
  ServerWorkspaceStatus,
  ServerWorkspaceTree,
  WorkspaceBinding
} from "../types/serverWorkspaceTypes";

export const serverWorkspacesKey = ["server-workspaces"] as const;
export const serverWorkspaceStatusKey = (workspaceId: string | null): readonly unknown[] =>
  ["server-workspaces", workspaceId, "status"];
export const serverWorkspaceTreeKey = (workspaceId: string, path: string): readonly unknown[] =>
  ["server-workspaces", workspaceId, "tree", path];
export const workspaceBindingsKey = (workspaceId: string | null): readonly unknown[] =>
  ["server-workspaces", workspaceId, "bindings"];

export const useServerWorkspaces = (enabled = true): UseQueryResult<ServerWorkspace[]> =>
  useQuery({ queryKey: serverWorkspacesKey, queryFn: listServerWorkspaces, enabled });

export const useServerWorkspaceStatus = (
  workspaceId: string | null
): UseQueryResult<ServerWorkspaceStatus> =>
  useQuery({
    queryKey: serverWorkspaceStatusKey(workspaceId),
    queryFn: (): Promise<ServerWorkspaceStatus> => getServerWorkspaceStatus(workspaceId ?? ""),
    enabled: workspaceId !== null,
    refetchOnWindowFocus: true
  });

export const useServerWorkspaceTree = (
  workspaceId: string,
  path: string,
  enabled = true,
  bindingId?: string
): UseQueryResult<ServerWorkspaceTree> =>
  useQuery({
    queryKey: [...serverWorkspaceTreeKey(workspaceId, path), bindingId ?? "server"],
    queryFn: (): Promise<ServerWorkspaceTree> => bindingId
      ? getLocalWorkspaceTree(workspaceId, bindingId, path)
      : getServerWorkspaceTree(workspaceId, path),
    enabled
  });

export const useWorkspaceBindings = (
  workspaceId: string | null
): UseQueryResult<WorkspaceBinding[]> =>
  useQuery({
    queryKey: workspaceBindingsKey(workspaceId),
    queryFn: (): Promise<WorkspaceBinding[]> => listWorkspaceBindings(workspaceId ?? ""),
    enabled: workspaceId !== null,
    refetchInterval: 10_000
  });

export const useCreateServerWorkspace = (): UseMutationResult<
  ServerWorkspace,
  Error,
  CreateServerWorkspaceInput
> => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createServerWorkspace,
    onSuccess: (): Promise<void> => queryClient.invalidateQueries({ queryKey: serverWorkspacesKey })
  });
};

export const useDeleteServerWorkspace = (): UseMutationResult<void, Error, string> => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteServerWorkspace,
    onSuccess: (): Promise<void> => queryClient.invalidateQueries({ queryKey: serverWorkspacesKey })
  });
};

export const useRefreshServerWorkspace = (): UseMutationResult<ServerWorkspaceStatus, Error, string> => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: refreshServerWorkspace,
    onSuccess: (_status, workspaceId): Promise<unknown> => Promise.all([
      queryClient.invalidateQueries({ queryKey: serverWorkspacesKey }),
      queryClient.invalidateQueries({ queryKey: serverWorkspaceStatusKey(workspaceId) }),
      queryClient.invalidateQueries({ queryKey: ["server-workspaces", workspaceId, "tree"] })
    ])
  });
};
