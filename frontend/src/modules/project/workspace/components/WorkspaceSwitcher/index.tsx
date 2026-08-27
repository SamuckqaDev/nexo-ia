import { CaretRight, FolderOpen, SlidersHorizontal } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { useActiveWorkspace } from "../../hooks/useActiveWorkspace";
import { useServerWorkspaces } from "../../hooks/useServerWorkspaces";
import type { WorkspaceSwitcherProps } from "../../types/workspaceTypes";
import { CollapsedButton, Copy, Label, ManageButton, Switcher } from "./styles";

export function WorkspaceSwitcher({ collapsed, onManage }: WorkspaceSwitcherProps): ReactElement {
  const workspaces = useServerWorkspaces();
  const activeWorkspace = useActiveWorkspace();
  const count = workspaces.data?.length ?? 0;

  if (collapsed) {
    return (
      <CollapsedButton
        type="button"
        title={activeWorkspace?.name ?? `${count} server workspace${count === 1 ? "" : "s"}`}
        aria-label="Manage server workspaces"
        onClick={onManage}
      >
        <FolderOpen size={20} weight={count ? "fill" : "duotone"} />
      </CollapsedButton>
    );
  }

  return (
    <Switcher>
      <Label>Server workspaces</Label>
      <div>
        <FolderOpen size={18} weight={count ? "fill" : "duotone"} />
        <Copy>
          <strong>{workspaces.isLoading ? "Loading projects…" : activeWorkspace?.name ?? `${count} registered`}</strong>
          <span>{activeWorkspace ? "Active project context" : "Choose a project workspace"}</span>
        </Copy>
        <ManageButton type="button" title="Manage workspaces" aria-label="Manage workspaces" onClick={onManage}>
          {count ? <SlidersHorizontal size={16} /> : <CaretRight size={16} />}
        </ManageButton>
      </div>
    </Switcher>
  );
}
