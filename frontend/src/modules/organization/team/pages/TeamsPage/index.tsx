import { Plus, UsersThree } from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { WorkspacePage } from "../../../../../shared/components/WorkspacePage";
import { useBackendVaultCatalog } from "../../../../knowledge/vault/hooks/useBackendVaultCatalog";
import type { BackendVault } from "../../../../knowledge/vault/types/backendVaultTypes";
import { useTeams } from "../../hooks/useTeams";
import { useTeamWorkspace } from "../../hooks/useTeamWorkspace";
import type { TeamsPageProps } from "../../types/teamPageTypes";
import type {
  AddTeamMemberValues,
  CreateTeamValues,
  CreateTeamVaultValues,
  Team,
  TeamMember
} from "../../types/teamTypes";
import { TeamDetails } from "./components/TeamDetails";
import { TeamExplorer } from "./components/TeamExplorer";
import { PageActions, Workspace } from "./styles";

export function TeamsPage({ user }: TeamsPageProps): ReactElement {
  const teamsState = useTeams();
  const vaultCatalog = useBackendVaultCatalog();
  const teams: Team[] = teamsState.teams.data ?? [];
  const vaults: BackendVault[] = vaultCatalog.vaults.data ?? [];
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [creating, setCreating] = useState<boolean>(false);
  const selected: Team | undefined = teams.find((team: Team) => team.id === selectedId);
  const teamWorkspace = useTeamWorkspace(selected?.id ?? null, Boolean(selected?.manageable));
  const members: TeamMember[] = teamWorkspace.members.data ?? [];
  const teamVaults: BackendVault[] = useMemo(
    () => vaults.filter((vault: BackendVault) => vault.ownerType === "TEAM" && vault.ownerId === selected?.id),
    [selected?.id, vaults]
  );
  const canCreateTeam: boolean = user.role === "OWNER" || user.role === "ADMIN";

  useEffect((): void => {
    if (creating || selected || !teams.length) return;
    setSelectedId(teams[0].id);
  }, [creating, selected, teams]);

  const selectTeam = (teamId: string): void => {
    setSelectedId(teamId);
    setCreating(false);
  };
  const createTeam = (values: CreateTeamValues): void => {
    teamsState.create.mutate(values, {
      onSuccess: (team: Team): void => {
        setSelectedId(team.id);
        setCreating(false);
      }
    });
  };
  const addMember = (values: AddTeamMemberValues): void => teamWorkspace.addMember.mutate(values);
  const createVault = (values: CreateTeamVaultValues): void => {
    if (!selected) return;
    vaultCatalog.createTeam.mutate({ teamId: selected.id, ...values });
  };

  return (
    <WorkspacePage
      eyebrow="Shared governance"
      title="Teams & shared knowledge"
      description="Organize people, capability profiles and Team-owned Vaults in one visible workspace. Members retrieve shared knowledge without taking ownership of it."
      icon={UsersThree}
      contentMode="contained"
      actions={canCreateTeam ? (
        <PageActions>
          <Button type="button" size="compact" icon={Plus} onClick={(): void => setCreating(true)}>New Team</Button>
        </PageActions>
      ) : undefined}
    >
      <Workspace>
        <TeamExplorer
          teams={teams}
          selectedId={selectedId}
          creating={creating}
          loading={teamsState.teams.isLoading}
          canCreateTeam={canCreateTeam}
          onSelect={selectTeam}
          onCreate={(): void => setCreating(true)}
        />
        <TeamDetails
          creating={creating}
          selected={selected}
          members={members}
          teamVaults={teamVaults}
          candidates={teamWorkspace.candidates.data ?? []}
          createPending={teamsState.create.isPending}
          membersLoading={teamWorkspace.members.isLoading}
          memberPending={teamWorkspace.addMember.isPending}
          vaultPending={vaultCatalog.createTeam.isPending}
          canAppointAdmin={user.role === "OWNER"}
          onCreateTeam={createTeam}
          onCancelCreate={(): void => setCreating(false)}
          onAddMember={addMember}
          onCreateVault={createVault}
        />
      </Workspace>
    </WorkspacePage>
  );
}
