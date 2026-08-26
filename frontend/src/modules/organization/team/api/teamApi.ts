import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { backendVaultSchema } from "../../../knowledge/vault/schemas/vaultSchemas";
import type { BackendVault } from "../../../knowledge/vault/types/backendVaultTypes";
import { teamCandidateSchema, teamMemberSchema, teamSchema } from "../schemas/teamSchemas";
import type {
  AddTeamMemberInput,
  CreateTeamInput,
  CreateTeamVaultValues,
  Team,
  TeamCandidate,
  TeamMember
} from "../types/teamTypes";

const first = <T>(response: BaseResponse<unknown>, parse: (value: unknown) => T): T => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty response");
  return parse(value);
};

export const listTeams = (): Promise<Team[]> =>
  apiClient.get<BaseResponse<unknown>>("/teams")
    .then(({ data }) => (data.data ?? []).map((item: unknown) => teamSchema.parse(item)));

export const createTeam = (input: CreateTeamInput): Promise<Team> =>
  apiClient.post<BaseResponse<unknown>>("/teams", input)
    .then(({ data }) => first(data, (value: unknown) => teamSchema.parse(value)));

export const listTeamMembers = (teamId: string): Promise<TeamMember[]> =>
  apiClient.get<BaseResponse<unknown>>(`/teams/${teamId}/members`)
    .then(({ data }) => (data.data ?? []).map((item: unknown) => teamMemberSchema.parse(item)));

export const listTeamCandidates = (teamId: string): Promise<TeamCandidate[]> =>
  apiClient.get<BaseResponse<unknown>>(`/teams/${teamId}/candidates`)
    .then(({ data }) => (data.data ?? []).map((item: unknown) => teamCandidateSchema.parse(item)));

export const addTeamMember = (teamId: string, input: AddTeamMemberInput): Promise<TeamMember> =>
  apiClient.post<BaseResponse<unknown>>(`/teams/${teamId}/members`, input)
    .then(({ data }) => first(data, (value: unknown) => teamMemberSchema.parse(value)));

export const createTeamVault = (teamId: string, input: CreateTeamVaultValues): Promise<BackendVault> =>
  apiClient.post<BaseResponse<unknown>>(`/teams/${teamId}/vaults`, input)
    .then(({ data }) => first(data, (value: unknown) => backendVaultSchema.parse(value)));
