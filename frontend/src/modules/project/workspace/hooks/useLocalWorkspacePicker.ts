import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useDesktopRuntime } from "../../../device/runtime/hooks/useDesktopRuntime";
import type { DesktopRuntimeHook } from "../../../device/runtime/hooks/useDesktopRuntime";
import type { ServerWorkspace } from "../types/serverWorkspaceTypes";
import { provisionLocalWorkspace } from "../services/localWorkspaceProvisioningService";
import { serverWorkspacesKey, workspaceBindingsKey } from "./useServerWorkspaces";

export type LocalWorkspacePickerHook = {
  available: boolean;
  pending: boolean;
  error: string | null;
  chooseLocalWorkspace: () => Promise<ServerWorkspace | null>;
};

const cacheWorkspace = (queryClient: QueryClient, workspace: ServerWorkspace): void => {
  queryClient.setQueryData<ServerWorkspace[]>(serverWorkspacesKey, (current: ServerWorkspace[] | undefined) => [
    workspace,
    ...(current ?? []).filter((item: ServerWorkspace): boolean => item.id !== workspace.id)
  ]);
};

export const useLocalWorkspacePicker = (): LocalWorkspacePickerHook => {
  const runtime: DesktopRuntimeHook = useDesktopRuntime();
  const queryClient: QueryClient = useQueryClient();
  const [pending, setPending] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const chooseLocalWorkspace = (): Promise<ServerWorkspace | null> => {
    if (!runtime.available) {
      const message = "Open Nexo in the Desktop application to choose a folder from this computer";
      setError(message);
      return Promise.reject(new Error(message));
    }

    setPending(true);
    setError(null);
    return provisionLocalWorkspace(runtime)
      .then((workspace: ServerWorkspace | null): Promise<ServerWorkspace | null> => {
        if (!workspace) return Promise.resolve(null);
        cacheWorkspace(queryClient, workspace);
        return Promise.all([
          queryClient.invalidateQueries({ queryKey: serverWorkspacesKey }),
          queryClient.invalidateQueries({ queryKey: workspaceBindingsKey(workspace.id) })
        ]).then(() => workspace);
      })
      .catch((reason: unknown): Promise<never> => {
        const message = reason instanceof Error ? reason.message : "Nexo could not register the selected workspace";
        setError(message);
        return Promise.reject(new Error(message));
      })
      .finally((): void => setPending(false));
  };

  return {
    available: runtime.available,
    pending: pending || runtime.pending,
    error: error ?? runtime.error,
    chooseLocalWorkspace
  };
};
