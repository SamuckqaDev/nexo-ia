import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { createTeam, listTeams } from "../api/teamApi";
import type { CreateTeamInput, Team } from "../types/teamTypes";

export const teamsKey = ["organization", "teams"] as const;

export function useTeams(enabled = true): {
  teams: UseQueryResult<Team[]>;
  create: UseMutationResult<Team, Error, CreateTeamInput>;
} {
  const queryClient: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const teams = useQuery({ queryKey: teamsKey, queryFn: listTeams, enabled, retry: false });
  const create = useMutation({
    mutationFn: createTeam,
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: teamsKey });
      show("Team created.", { variant: "success" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });
  return { teams, create };
}
