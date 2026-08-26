import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { addTeamMember, listTeamCandidates, listTeamMembers } from "../api/teamApi";
import type { AddTeamMemberInput, TeamCandidate, TeamMember } from "../types/teamTypes";

export const teamMembersKey = (teamId: string | null) => ["organization", "teams", teamId, "members"] as const;
export const teamCandidatesKey = (teamId: string | null) => ["organization", "teams", teamId, "candidates"] as const;

export function useTeamWorkspace(teamId: string | null, manageable: boolean): {
  members: UseQueryResult<TeamMember[]>;
  candidates: UseQueryResult<TeamCandidate[]>;
  addMember: UseMutationResult<TeamMember, Error, AddTeamMemberInput>;
} {
  const queryClient: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const members = useQuery({
    queryKey: teamMembersKey(teamId),
    queryFn: (): Promise<TeamMember[]> => listTeamMembers(teamId as string),
    enabled: Boolean(teamId),
    retry: false
  });
  const candidates = useQuery({
    queryKey: teamCandidatesKey(teamId),
    queryFn: (): Promise<TeamCandidate[]> => listTeamCandidates(teamId as string),
    enabled: Boolean(teamId) && manageable,
    retry: false
  });
  const addMember = useMutation({
    mutationFn: (input: AddTeamMemberInput): Promise<TeamMember> => addTeamMember(teamId as string, input),
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: teamMembersKey(teamId) });
      queryClient.invalidateQueries({ queryKey: teamCandidatesKey(teamId) });
      show("Member added to the Team.", { variant: "success" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });
  return { members, candidates, addMember };
}
