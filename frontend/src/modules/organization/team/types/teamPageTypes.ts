import type { AuthenticatedUser } from "../../../auth/types/authTypes";
import type { BackendVault } from "../../../knowledge/vault/types/backendVaultTypes";
import type {
  AddTeamMemberValues,
  CreateTeamValues,
  CreateTeamVaultValues,
  Team,
  TeamCandidate,
  TeamMember
} from "./teamTypes";

export type TeamExplorerProps = {
  teams: Team[];
  selectedId: string | null;
  creating: boolean;
  loading: boolean;
  canCreateTeam: boolean;
  onSelect: (teamId: string) => void;
  onCreate: () => void;
};

export type TeamsPageProps = { user: AuthenticatedUser };

export type TeamDetailsProps = {
  creating: boolean;
  selected?: Team;
  members: TeamMember[];
  teamVaults: BackendVault[];
  candidates: TeamCandidate[];
  createPending: boolean;
  membersLoading: boolean;
  memberPending: boolean;
  vaultPending: boolean;
  canAppointAdmin: boolean;
  onCreateTeam: (values: CreateTeamValues) => void;
  onCancelCreate: () => void;
  onAddMember: (values: AddTeamMemberValues) => void;
  onCreateVault: (values: CreateTeamVaultValues) => void;
};
