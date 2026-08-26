import {
  BookOpen,
  Buildings,
  Crown,
  Plus,
  ShieldCheck,
  User,
  UsersThree
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type ReactElement } from "react";
import type { AuthenticatedUser } from "../../../../auth/types/authTypes";
import { useBackendVaultCatalog } from "../../../../knowledge/vault/hooks/useBackendVaultCatalog";
import type { BackendVault } from "../../../../knowledge/vault/types/backendVaultTypes";
import { Button } from "../../../../../shared/components/Button";
import { Loading } from "../../../../../shared/components/Loading";
import {
  WorkspaceBadge,
  WorkspaceEmptyState,
  WorkspacePage,
  WorkspacePanel
} from "../../../../../shared/components/WorkspacePage";
import { CreateTeamForm } from "../../components/CreateTeamForm";
import { TeamAdminForms } from "../../components/TeamAdminForms";
import { useTeams } from "../../hooks/useTeams";
import { useTeamWorkspace } from "../../hooks/useTeamWorkspace";
import type {
  AddTeamMemberValues,
  CreateTeamValues,
  CreateTeamVaultValues,
  Team,
  TeamMember
} from "../../types/teamTypes";
import {
  AdminArea,
  Detail,
  DetailScroll,
  EmptyList,
  Library,
  MemberCard,
  MemberGrid,
  MetaGrid,
  MetaItem,
  PageActions,
  SectionHeading,
  SharedVault,
  SharedVaultGrid,
  TeamButton,
  TeamCopy,
  TeamList,
  Workspace
} from "./styles";

type TeamsPageProps = { user: AuthenticatedUser };

const formatBudget = (budget: number | null): string =>
  budget === null ? "Not allocated" : new Intl.NumberFormat("en-US").format(budget);

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
        <PageActions><Button type="button" size="compact" icon={Plus} onClick={(): void => setCreating(true)}>New Team</Button></PageActions>
      ) : undefined}
    >
      <Workspace>
        <WorkspacePanel title="Your Teams" description="Organizations you belong to and your role in each one.">
          <Library>
            {teamsState.teams.isLoading ? <Loading label="Loading Teams…" /> : teams.length ? (
              <TeamList>
                {teams.map((team: Team) => (
                  <TeamButton
                    key={team.id}
                    type="button"
                    $active={selected?.id === team.id && !creating}
                    onClick={(): void => { setSelectedId(team.id); setCreating(false); }}
                  >
                    <Buildings size={19} weight="duotone" />
                    <TeamCopy><strong>{team.name}</strong><span>{team.teamRole.toLowerCase()} · {team.assignedProfile.toLowerCase()}</span></TeamCopy>
                    {team.manageable ? <Crown size={14} weight="fill" /> : <User size={14} />}
                  </TeamButton>
                ))}
              </TeamList>
            ) : (
              <EmptyList>
                <WorkspaceEmptyState
                  icon={UsersThree}
                  title="No Teams yet"
                  description={canCreateTeam ? "Create a Team to share governed knowledge and capabilities." : "An administrator must add you to a Team."}
                  action={canCreateTeam ? <Button type="button" size="compact" icon={Plus} onClick={(): void => setCreating(true)}>Create Team</Button> : undefined}
                />
              </EmptyList>
            )}
          </Library>
        </WorkspacePanel>

        <WorkspacePanel
          title={creating ? "Create a Team" : selected?.name ?? "Select a Team"}
          description={creating ? "Set the default capability boundary and an optional allocation." : selected ? `${selected.teamRole.toLowerCase()} access in this organization.` : undefined}
          action={selected && !creating ? <WorkspaceBadge tone={selected.manageable ? "positive" : "default"}>{selected.manageable ? "Administrator" : "Member"}</WorkspaceBadge> : undefined}
        >
          {creating ? (
            <CreateTeamForm pending={teamsState.create.isPending} onSubmit={createTeam} onCancel={(): void => setCreating(false)} />
          ) : selected ? (
            <Detail>
              <DetailScroll>
                <MetaGrid>
                  <MetaItem><ShieldCheck size={18} /><span>Default profile</span><strong>{selected.defaultProfile.toLowerCase()}</strong></MetaItem>
                  <MetaItem><User size={18} /><span>Your profile</span><strong>{selected.assignedProfile.toLowerCase()}</strong></MetaItem>
                  <MetaItem><Crown size={18} /><span>Team role</span><strong>{selected.teamRole.toLowerCase()}</strong></MetaItem>
                  <MetaItem><BookOpen size={18} /><span>Token allocation</span><strong>{formatBudget(selected.tokenBudgetLimit)}</strong></MetaItem>
                </MetaGrid>

                <SectionHeading><div><h3>Members</h3><p>Identity, Team authority and the capability profile applied in this organization.</p></div><WorkspaceBadge>{members.length} people</WorkspaceBadge></SectionHeading>
                {teamWorkspace.members.isLoading ? <Loading label="Loading members…" /> : (
                  <MemberGrid>
                    {members.map((member: TeamMember) => (
                      <MemberCard key={member.userId}>
                        <span><strong>{member.name}</strong><small>@{member.username} · {member.email}</small></span>
                        <div><WorkspaceBadge tone={member.teamRole === "ADMIN" ? "positive" : "default"}>{member.teamRole.toLowerCase()}</WorkspaceBadge><WorkspaceBadge>{member.assignedProfile.toLowerCase()}</WorkspaceBadge></div>
                      </MemberCard>
                    ))}
                  </MemberGrid>
                )}

                <SectionHeading><div><h3>Shared Knowledge Vaults</h3><p>These collections belong to {selected.name}, not to an individual account.</p></div><WorkspaceBadge tone="positive">{teamVaults.length} Vaults</WorkspaceBadge></SectionHeading>
                {teamVaults.length ? (
                  <SharedVaultGrid>
                    {teamVaults.map((vault: BackendVault) => (
                      <SharedVault key={vault.id}>
                        <BookOpen size={19} weight="duotone" />
                        <span><strong>{vault.name}</strong><small>{vault.description ?? "Shared Team knowledge"}</small></span>
                        <WorkspaceBadge>{vault.manageable ? "Manage" : "Read"}</WorkspaceBadge>
                      </SharedVault>
                    ))}
                  </SharedVaultGrid>
                ) : <WorkspaceEmptyState icon={BookOpen} title="No shared Vaults" description={selected.manageable ? "Create the first Team-owned knowledge collection below." : "A Team administrator has not shared knowledge yet."} />}

                {selected.manageable && (
                  <AdminArea>
                    <SectionHeading><div><h3>Team administration</h3><p>Add people and create shared knowledge without leaving this workspace.</p></div></SectionHeading>
                    <TeamAdminForms
                      candidates={teamWorkspace.candidates.data ?? []}
                      canAppointAdmin={user.role === "OWNER"}
                      memberPending={teamWorkspace.addMember.isPending}
                      vaultPending={vaultCatalog.createTeam.isPending}
                      onAddMember={addMember}
                      onCreateVault={createVault}
                    />
                  </AdminArea>
                )}
              </DetailScroll>
            </Detail>
          ) : <WorkspaceEmptyState icon={UsersThree} title="Select a Team" description="Choose an organization from the list to inspect its people and knowledge." />}
        </WorkspacePanel>
      </Workspace>
    </WorkspacePage>
  );
}
