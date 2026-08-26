import { Buildings, Crown, Plus, User, UsersThree } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../../../shared/components/Button";
import { Loading } from "../../../../../../../shared/components/Loading";
import {
  WorkspaceEmptyState,
  WorkspacePanel
} from "../../../../../../../shared/components/WorkspacePage";
import type { TeamExplorerProps } from "../../../../types/teamPageTypes";
import type { Team } from "../../../../types/teamTypes";
import { EmptyList, Library, TeamButton, TeamCopy, TeamList } from "./styles";

export function TeamExplorer({
  teams,
  selectedId,
  creating,
  loading,
  canCreateTeam,
  onSelect,
  onCreate
}: TeamExplorerProps): ReactElement {
  return (
    <WorkspacePanel title="Your Teams" description="Organizations you belong to and your role in each one.">
      <Library>
        {loading ? <Loading label="Loading Teams…" /> : teams.length ? (
          <TeamList>
            {teams.map((team: Team) => (
              <TeamButton
                key={team.id}
                type="button"
                $active={selectedId === team.id && !creating}
                $manageable={team.manageable}
                onClick={(): void => onSelect(team.id)}
              >
                <Buildings size={19} weight="duotone" />
                <TeamCopy>
                  <strong>{team.name}</strong>
                  <span>{team.teamRole.toLowerCase()} · {team.assignedProfile.toLowerCase()}</span>
                </TeamCopy>
                {team.manageable ? <Crown size={14} weight="fill" /> : <User size={14} />}
              </TeamButton>
            ))}
          </TeamList>
        ) : (
          <EmptyList>
            <WorkspaceEmptyState
              icon={UsersThree}
              title="No Teams yet"
              description={canCreateTeam
                ? "Create a Team to share governed knowledge and capabilities."
                : "An administrator must add you to a Team."}
              action={canCreateTeam
                ? <Button type="button" size="compact" icon={Plus} onClick={onCreate}>Create Team</Button>
                : undefined}
            />
          </EmptyList>
        )}
      </Library>
    </WorkspacePanel>
  );
}
