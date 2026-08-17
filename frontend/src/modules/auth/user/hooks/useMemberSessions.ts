import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseQueryResult } from "@tanstack/react-query";
import type { ActiveSession } from "../../types/sessionTypes";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { getMemberSessions, revokeMemberSession } from "../api/userManagementApi";

export function useMemberSessions(userId: string): {
  query: UseQueryResult<ActiveSession[], Error>;
  revoke: (sessionId: string) => void;
  isRevoking: boolean;
} {
  const client: QueryClient = useQueryClient();
  const key = ["admin", "users", userId, "sessions"] as const;
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const query = useQuery({ queryKey: key, queryFn: (): Promise<ActiveSession[]> => getMemberSessions(userId) });
  const mutation = useMutation({
    mutationFn: (sessionId: string): Promise<void> => revokeMemberSession(userId, sessionId),
    onSuccess: (): void => { client.invalidateQueries({ queryKey: key }); show("Member session revoked.", { variant: "success" }); },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });
  return { query, revoke: (sessionId: string): void => mutation.mutate(sessionId), isRevoking: mutation.isPending };
}
