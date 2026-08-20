import { CaretRight, FolderOpen, SlidersHorizontal } from "@phosphor-icons/react";
import type { ChangeEvent, ReactElement } from "react";
import { useActiveWorkspace } from "../../hooks/useActiveWorkspace";
import { workspacePlatformLabel } from "../../services/workspacePlatformService";
import { useWorkspaceStore } from "../../stores/useWorkspaceStore";
import type { ProjectWorkspace, WorkspaceState, WorkspaceSwitcherProps } from "../../types/workspaceTypes";
import { CollapsedButton, Copy, Label, ManageButton, Select, Switcher } from "./styles";

export function WorkspaceSwitcher({ collapsed, onManage }: WorkspaceSwitcherProps): ReactElement {
  const active = useActiveWorkspace();
  const workspaces: ProjectWorkspace[] = useWorkspaceStore((state: WorkspaceState) => state.workspaces);
  const selectWorkspace: WorkspaceState["selectWorkspace"] = useWorkspaceStore((state: WorkspaceState) => state.selectWorkspace);

  if (collapsed) {
    return (
      <CollapsedButton type="button" title={active ? `Workspace: ${active.name}` : "Choose workspace"} aria-label={active ? `Manage workspace ${active.name}` : "Choose workspace"} onClick={onManage}>
        <FolderOpen size={20} weight={active ? "fill" : "duotone"} />
      </CollapsedButton>
    );
  }

  return (
    <Switcher>
      <Label htmlFor="active-workspace">Active workspace</Label>
      <div>
        <FolderOpen size={18} weight={active ? "fill" : "duotone"} />
        <Copy>
          <Select
            id="active-workspace"
            value={active?.id ?? ""}
            onChange={(event: ChangeEvent<HTMLSelectElement>): void => selectWorkspace(event.target.value || null)}
          >
            <option value="">{workspaces.length ? "No workspace" : "Choose a project folder"}</option>
            {workspaces.map((workspace: ProjectWorkspace) => <option key={workspace.id} value={workspace.id}>{workspace.name}</option>)}
          </Select>
          <span>{active ? `${active.directoryName} · ${workspacePlatformLabel(active.platform)}` : "Chat without project context"}</span>
        </Copy>
        <ManageButton type="button" title="Manage workspaces" aria-label="Manage workspaces" onClick={onManage}>
          {workspaces.length ? <SlidersHorizontal size={16} /> : <CaretRight size={16} />}
        </ManageButton>
      </div>
    </Switcher>
  );
}
