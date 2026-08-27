import { FolderOpen, FolderSimplePlus, SpinnerGap } from "@phosphor-icons/react";
import type { ChangeEvent, ReactElement } from "react";
import type { ServerWorkspace } from "../../types/serverWorkspaceTypes";
import { Context, FolderButton, WorkspaceSelect } from "./styles";

type WorkspacePickerControlProps = {
  workspaceId: string | null;
  workspaces: ServerWorkspace[];
  selectDisabled: boolean;
  localDisabled: boolean;
  localAvailable: boolean;
  localPending: boolean;
  onSelect: (workspaceId: string | null) => void;
  onChooseLocal: () => void;
};

export function WorkspacePickerControl({
  workspaceId,
  workspaces,
  selectDisabled,
  localDisabled,
  localAvailable,
  localPending,
  onSelect,
  onChooseLocal
}: WorkspacePickerControlProps): ReactElement {
  return (
    <Context $active={Boolean(workspaceId)} title="Workspace persisted for this conversation">
      <FolderOpen size={15} weight={workspaceId ? "fill" : "duotone"} />
      <WorkspaceSelect
        aria-label="Conversation workspace"
        value={workspaceId ?? ""}
        disabled={selectDisabled}
        onChange={(event: ChangeEvent<HTMLSelectElement>): void => onSelect(event.target.value || null)}
      >
        <option value="">No workspace</option>
        {workspaces.map((workspace: ServerWorkspace) => (
          <option key={workspace.id} value={workspace.id}>{workspace.name}</option>
        ))}
      </WorkspaceSelect>
      <FolderButton
        type="button"
        $pending={localPending}
        aria-label="Choose a project folder from this computer"
        title={localAvailable
          ? "Open Finder or the system folder chooser"
          : "Open Projects to configure Nexo Desktop"}
        disabled={localDisabled || localPending}
        onClick={onChooseLocal}
      >
        {localPending
          ? <SpinnerGap size={14} weight="bold" />
          : <FolderSimplePlus size={15} weight="duotone" />}
      </FolderButton>
    </Context>
  );
}
