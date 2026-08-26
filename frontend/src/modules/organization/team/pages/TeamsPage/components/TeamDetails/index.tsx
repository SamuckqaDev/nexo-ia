import { BookOpen, Crown, ShieldCheck, User, UsersThree } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Loading } from "../../../../../../../shared/components/Loading";
import {
  WorkspaceBadge,
  WorkspaceEmptyState,
  WorkspacePanel
} from "../../../../../../../shared/components/WorkspacePage";
import type { BackendVault } from "../../../../../../knowledge/vault/types/backendVaultTypes";
import { CreateTeamForm } from "../../../../components/CreateTeamForm";
import { TeamAdminForms } from "../../../../components/TeamAdminForms";
import type { TeamDetailsProps } from "../../../../types/teamPageTypes";
import type { TeamMember } from "../../../../types/teamTypes";
import {
  AdminArea,
  Detail,
  DetailScroll,
  MemberCard,
  MemberGrid,
  MetaGrid,
  MetaItem,
  SectionHeading,
  SharedVault,
  SharedVaultGrid
} from "./styles";

const formatBudget = (budget: number | null): string =>
  budget === null ? "Not allocated" : new Intl.NumberFormat("en-US").format(budget);

export function TeamDetails({
  creating,
  selected,
  members,
  teamVaults,
  candidates,
  createPending,
  membersLoading,
  memberPending,
  vaultPending,
  canAppointAdmin,
  onCreateTeam,
  onCancelCreate,
  onAddMember,
  onCreateVault
}: TeamDetailsProps): ReactElement {
  return (
    <WorkspacePanel
      title={creating ? "Create a Team" : selected?.name ?? "Select a Team"}
      description={creating
        ? "Set the default capability boundary and an optional allocation."
        : selected ? `${selected.teamRole.toLowerCase()} access in this organization.` : undefined}
      action={selected && !creating
        ? <WorkspaceBadge tone={selected.manageable ? "positive" : "default"}>{selected.manageable ? "Administrator" : "Member"}</WorkspaceBadge>
        : undefined}
    >
      {creating ? (
        <CreateTeamForm pending={createPending} onSubmit={onCreateTeam} onCancel={onCancelCreate} />
      ) : selected ? (
        <Detail>
          <DetailScroll>
            <MetaGrid>
              <MetaItem><ShieldCheck size={18} /><span>Default profile</span><strong>{selected.defaultProfile.toLowerCase()}</strong></MetaItem>
              <MetaItem><User size={18} /><span>Your profile</span><strong>{selected.assignedProfile.toLowerCase()}</strong></MetaItem>
              <MetaItem><Crown size={18} /><span>Team role</span><strong>{selected.teamRole.toLowerCase()}</strong></MetaItem>
              <MetaItem><BookOpen size={18} /><span>Token allocation</span><strong>{formatBudget(selected.tokenBudgetLimit)}</strong></MetaItem>
            </MetaGrid>

            <SectionHeading>
              <div><h3>Members</h3><p>Identity, Team authority and the capability profile applied in this organization.</p></div>
              <WorkspaceBadge>{members.length} people</WorkspaceBadge>
            </SectionHeading>
            {membersLoading ? <Loading label="Loading members…" /> : (
              <MemberGrid>
                {members.map((member: TeamMember) => (
                  <MemberCard key={member.userId}>
                    <span><strong>{member.name}</strong><small>@{member.username} · {member.email}</small></span>
                    <div>
                      <WorkspaceBadge tone={member.teamRole === "ADMIN" ? "positive" : "default"}>{member.teamRole.toLowerCase()}</WorkspaceBadge>
                      <WorkspaceBadge>{member.assignedProfile.toLowerCase()}</WorkspaceBadge>
                    </div>
                  </MemberCard>
                ))}
              </MemberGrid>
            )}

            <SectionHeading>
              <div><h3>Shared Knowledge Vaults</h3><p>These collections belong to {selected.name}, not to an individual account.</p></div>
              <WorkspaceBadge tone="positive">{teamVaults.length} Vaults</WorkspaceBadge>
            </SectionHeading>
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
            ) : (
              <WorkspaceEmptyState
                icon={BookOpen}
                title="No shared Vaults"
                description={selected.manageable
                  ? "Create the first Team-owned knowledge collection below."
                  : "A Team administrator has not shared knowledge yet."}
              />
            )}

            {selected.manageable && (
              <AdminArea>
                <SectionHeading>
                  <div><h3>Team administration</h3><p>Add people and create shared knowledge without leaving this workspace.</p></div>
                </SectionHeading>
                <TeamAdminForms
                  candidates={candidates}
                  canAppointAdmin={canAppointAdmin}
                  memberPending={memberPending}
                  vaultPending={vaultPending}
                  onAddMember={onAddMember}
                  onCreateVault={onCreateVault}
                />
              </AdminArea>
            )}
          </DetailScroll>
        </Detail>
      ) : (
        <WorkspaceEmptyState
          icon={UsersThree}
          title="Select a Team"
          description="Choose an organization from the list to inspect its people and knowledge."
        />
      )}
    </WorkspacePanel>
  );
}
