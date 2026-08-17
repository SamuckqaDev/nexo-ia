import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { getBootstrapStatus, getCurrentUser, logout } from "../api/authApi";
import type { AuthenticatedUser, AuthSessionResult } from "../../types/authTypes";

export function useAuthSession(): AuthSessionResult {
  const queryClient: QueryClient = useQueryClient();
  const showSnackbar: SnackbarState["show"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["show"] => state.show
  );
  const bootstrap: UseQueryResult<boolean, Error> = useQuery({
    queryKey: ["auth", "bootstrap"],
    queryFn: getBootstrapStatus
  });
  const session: UseQueryResult<AuthenticatedUser | null, Error> = useQuery({
    queryKey: ["auth", "session"],
    queryFn: getCurrentUser,
    enabled: bootstrap.data === false,
    retry: false
  });
  const logoutMutation: UseMutationResult<void, Error, void> = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(["auth", "session"], null);
      showSnackbar("Session ended safely.", { variant: "info" });
    },
    onError: (error) => showSnackbar(error.message, { variant: "error" })
  });

  return {
    bootstrapRequired: bootstrap.data,
    user: session.data,
    isLoading: bootstrap.isLoading || (bootstrap.data === false && session.isLoading),
    error: bootstrap.error ?? session.error,
    logout: (): void => logoutMutation.mutate(),
    isLoggingOut: logoutMutation.isPending
  };
}
