import type { z } from "zod";
import type {
  addTeamMemberSchema,
  createTeamSchema,
  createTeamVaultSchema,
  profileKeySchema,
  teamCandidateSchema,
  teamMemberSchema,
  teamRoleSchema,
  teamSchema
} from "../schemas/teamSchemas";

export type ProfileKey = z.infer<typeof profileKeySchema>;
export type TeamRole = z.infer<typeof teamRoleSchema>;
export type Team = z.infer<typeof teamSchema>;
export type TeamMember = z.infer<typeof teamMemberSchema>;
export type TeamCandidate = z.infer<typeof teamCandidateSchema>;
export type CreateTeamValues = z.infer<typeof createTeamSchema>;
export type AddTeamMemberValues = z.infer<typeof addTeamMemberSchema>;
export type CreateTeamVaultValues = z.infer<typeof createTeamVaultSchema>;

export type CreateTeamInput = {
  name: string;
  defaultProfile: ProfileKey;
  tokenBudgetLimit?: number;
};

export type AddTeamMemberInput = {
  userId: string;
  teamRole: TeamRole;
  profile: ProfileKey;
};

export type CreateTeamFormProps = {
  pending: boolean;
  onSubmit: (values: CreateTeamValues) => void;
  onCancel: () => void;
};

export type TeamAdminFormsProps = {
  candidates: TeamCandidate[];
  canAppointAdmin: boolean;
  memberPending: boolean;
  vaultPending: boolean;
  onAddMember: (values: AddTeamMemberValues) => void;
  onCreateVault: (values: CreateTeamVaultValues) => void;
};
