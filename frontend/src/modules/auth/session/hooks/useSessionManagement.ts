import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import type { SessionManagementResult } from "../../types/sessionTypes";
import { getActiveSessions, revokeOtherSessions, revokeSession } from "../api/sessionApi";

const sessionsQueryKey = ["auth", "sessions"] as const;

export function useSessionManagement(): SessionManagementResult {
  const queryClient: QueryClient = useQueryClient();
  const showSnackbar: SnackbarState["show"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["show"] => state.show
  );
  const query: SessionManagementResult["query"] = useQuery({
    queryKey: sessionsQueryKey,
    queryFn: getActiveSessions
  });
  const revokeMutation: SessionManagementResult["revokeMutation"] = useMutation({
    mutationFn: revokeSession,
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: sessionsQueryKey });
      showSnackbar("Session revoked successfully.", { variant: "success" });
    },
    onError: (error: Error): void => showSnackbar(error.message, { variant: "error" })
  });
  const revoke: SessionManagementResult["revoke"] = (sessionId: string): void => {
    revokeMutation.mutate(sessionId);
  };
  const revokeOthersMutation = useMutation({
    mutationFn: revokeOtherSessions,
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: sessionsQueryKey });
      showSnackbar("All other sessions were revoked.", { variant: "success" });
    },
    onError: (error: Error): void => showSnackbar(error.message, { variant: "error" })
  });
  const revokeOthers = (): void => revokeOthersMutation.mutate();

  return { query, revokeMutation, revoke, revokeOthers,
    isRevokingOthers: revokeOthersMutation.isPending };
}
