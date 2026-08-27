import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import type { DesktopRuntimeState } from "../../../../shared/types/desktopTypes";
import { createDevicePairing, listDevices, revokeDevice } from "../api/deviceApi";
import type { Device } from "../types/deviceTypes";

const devicesKey = ["devices"] as const;
const emptyState: DesktopRuntimeState = {
  paired: false,
  connected: false,
  deviceId: null,
  serverUrl: null,
  workspaces: []
};

export const useDevices = (): UseQueryResult<Device[]> =>
  useQuery({ queryKey: devicesKey, queryFn: listDevices, refetchInterval: 15_000 });

export const useRevokeDevice = (): UseMutationResult<void, Error, string> => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: revokeDevice,
    onSuccess: (): Promise<void> => queryClient.invalidateQueries({ queryKey: devicesKey })
  });
};

export type DesktopRuntimeHook = {
  available: boolean;
  state: DesktopRuntimeState;
  pending: boolean;
  error: string | null;
  pair: () => void;
  chooseWorkspace: (workspaceId: string, workspaceName: string) => Promise<DesktopRuntimeState>;
};

export const useDesktopRuntime = (): DesktopRuntimeHook => {
  const bridge = window.nexoDesktop;
  const queryClient = useQueryClient();
  const [state, setState] = useState<DesktopRuntimeState>(emptyState);
  const [pending, setPending] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect((): (() => void) | void => {
    if (!bridge) return undefined;
    bridge.state().then(setState).catch((reason: unknown): void => {
      setError(reason instanceof Error ? reason.message : "Nexo Desktop is unavailable");
    });
    return bridge.onStatus(setState);
  }, [bridge]);

  const pair = (): void => {
    if (!bridge || pending) return;
    setPending(true);
    setError(null);
    createDevicePairing()
      .then((pairing) => bridge.pair({
        serverUrl: window.location.protocol === "file:" ? "http://127.0.0.1:8080" : window.location.origin,
        pairingCode: pairing.pairingCode,
        displayName: `Nexo Desktop · ${navigator.platform || "Computer"}`
      }))
      .then((next: DesktopRuntimeState): Promise<void> => {
        setState(next);
        return queryClient.invalidateQueries({ queryKey: devicesKey });
      })
      .catch((reason: unknown): void => {
        setError(reason instanceof Error ? reason.message : "Nexo Desktop could not be paired");
      })
      .finally((): void => setPending(false));
  };

  const chooseWorkspace = (workspaceId: string, workspaceName: string): Promise<DesktopRuntimeState> => {
    if (!bridge) return Promise.reject(new Error("Open Nexo in the Desktop application to choose a local folder"));
    setPending(true);
    setError(null);
    return bridge.chooseWorkspace({ workspaceId, workspaceName })
      .then((next: DesktopRuntimeState): DesktopRuntimeState => {
        setState(next);
        return next;
      })
      .catch((reason: unknown): Promise<never> => {
        const message = reason instanceof Error ? reason.message : "Nexo could not bind the local folder";
        setError(message);
        return Promise.reject(new Error(message));
      })
      .finally((): void => setPending(false));
  };

  return { available: Boolean(bridge), state, pending, error, pair, chooseWorkspace };
};
