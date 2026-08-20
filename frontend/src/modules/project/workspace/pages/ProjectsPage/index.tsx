import {
  ArrowRight,
  CheckCircle,
  FolderOpen,
  FolderSimplePlus,
  GitBranch,
  HardDrives,
  MagnifyingGlass,
  Plus,
  ShieldCheck
} from "@phosphor-icons/react";
import { useMemo, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { WorkspaceBadge, WorkspaceEmptyState, WorkspacePage, WorkspacePanel } from "../../../../../shared/components/WorkspacePage";
import { WorkspaceForm } from "../../components/WorkspaceForm";
import { useActiveWorkspace } from "../../hooks/useActiveWorkspace";
import { useWorkspaceStore } from "../../stores/useWorkspaceStore";
import type { ProjectWorkspace, ProjectsPageProps, WorkspaceState } from "../../types/workspaceTypes";
import {
  ActiveContext,
  Detail,
  DetailActions,
  DetailHeader,
  DetailMeta,
  Library,
  Path,
  ProjectsGrid,
  WorkspaceButton,
  WorkspaceCopy,
  WorkspaceList
} from "./styles";

export function ProjectsPage({ onOpenChat }: ProjectsPageProps): ReactElement {
  const workspaces: ProjectWorkspace[] = useWorkspaceStore((state: WorkspaceState) => state.workspaces);
  const selectWorkspace: WorkspaceState["selectWorkspace"] = useWorkspaceStore((state: WorkspaceState) => state.selectWorkspace);
  const active = useActiveWorkspace();
  const [selectedId, setSelectedId] = useState<string | null>(active?.id ?? workspaces[0]?.id ?? null);
  const [adding, setAdding] = useState<boolean>(workspaces.length === 0);
  const [query, setQuery] = useState<string>("");
  const selected: ProjectWorkspace | undefined = workspaces.find((workspace: ProjectWorkspace) => workspace.id === selectedId) ?? active;
  const visibleWorkspaces = useMemo<ProjectWorkspace[]>(() => workspaces.filter((workspace: ProjectWorkspace) =>
    `${workspace.name} ${workspace.path}`.toLowerCase().includes(query.toLowerCase())
  ), [query, workspaces]);

  const chooseWorkspace = (workspace: ProjectWorkspace): void => {
    setSelectedId(workspace.id);
    selectWorkspace(workspace.id);
    setAdding(false);
  };

  return (
    <WorkspacePage
      eyebrow="Project context"
      title="Projects & workspaces"
      description="Choose the exact project folder Nexo should treat as the working context, then enter Chat or Cowork with that selection visible everywhere."
      icon={FolderOpen}
      actions={<Button type="button" icon={Plus} onClick={(): void => setAdding(true)}>Add workspace</Button>}
    >
      {active && (
        <ActiveContext>
          <FolderOpen size={22} weight="fill" />
          <div><span>Active workspace</span><strong>{active.name}</strong><small>{active.path}</small></div>
          <WorkspaceBadge tone="positive">Selected</WorkspaceBadge>
          <Button type="button" icon={ArrowRight} onClick={onOpenChat}>Open Chat</Button>
        </ActiveContext>
      )}

      <ProjectsGrid>
        <WorkspacePanel title="Project folders" description="Switching here changes the session workspace shown across Nexo." action={<WorkspaceBadge>{workspaces.length} saved this session</WorkspaceBadge>}>
          <Library>
            {workspaces.length > 0 && (
              <Input id="workspace-search" label="Find a workspace" icon={MagnifyingGlass} value={query} onChange={(event): void => setQuery(event.target.value)} placeholder="Name or folder path" />
            )}
            {visibleWorkspaces.length > 0 ? (
              <WorkspaceList>
                {visibleWorkspaces.map((workspace: ProjectWorkspace) => (
                  <WorkspaceButton key={workspace.id} type="button" $active={active?.id === workspace.id} onClick={(): void => chooseWorkspace(workspace)}>
                    <FolderOpen size={21} weight={active?.id === workspace.id ? "fill" : "duotone"} />
                    <WorkspaceCopy>
                      <strong>{workspace.name}</strong>
                      <span>{workspace.path}</span>
                      <small>{active?.id === workspace.id ? "Active workspace" : "Select workspace"}</small>
                    </WorkspaceCopy>
                    {active?.id === workspace.id ? <CheckCircle size={18} weight="fill" /> : <ArrowRight size={16} />}
                  </WorkspaceButton>
                ))}
              </WorkspaceList>
            ) : (
              <WorkspaceEmptyState
                icon={FolderSimplePlus}
                title={workspaces.length ? "No matching workspace" : "Choose a project folder"}
                description={workspaces.length ? "Try another name or exact path." : "Add the folder you want Nexo to treat as the working directory. You can switch workspaces at any time from the sidebar."}
                action={!workspaces.length ? <Button type="button" icon={FolderSimplePlus} onClick={(): void => setAdding(true)}>Add first workspace</Button> : undefined}
              />
            )}
          </Library>
        </WorkspacePanel>

        <WorkspacePanel as="aside" title={adding ? "Add a project folder" : selected ? "Workspace details" : "No workspace selected"} description={adding ? "Name the folder and choose the initial session scope." : "Review the exact working context before starting."}>
          {adding ? (
            <WorkspaceForm onAdded={(workspace: ProjectWorkspace): void => { setSelectedId(workspace.id); setAdding(false); }} onCancel={(): void => setAdding(false)} />
          ) : selected ? (
            <Detail>
              <DetailHeader>
                <span><FolderOpen size={25} weight="duotone" /></span>
                <div><WorkspaceBadge tone={active?.id === selected.id ? "positive" : "default"}>{active?.id === selected.id ? "Active" : "Available"}</WorkspaceBadge><h2>{selected.name}</h2></div>
              </DetailHeader>
              <Path><span>Working directory</span><code>{selected.path}</code></Path>
              <DetailMeta>
                <div><HardDrives size={18} /><span>Session scope<strong>{selected.access}</strong></span></div>
                <div><GitBranch size={18} /><span>Git context<strong>{selected.branch ?? "Detect on connect"}</strong></span></div>
                <div><ShieldCheck size={18} /><span>Authorization<strong>Companion required</strong></span></div>
              </DetailMeta>
              <DetailActions>
                {active?.id !== selected.id && <Button type="button" onClick={(): void => selectWorkspace(selected.id)}>Use this workspace</Button>}
                <Button type="button" variant={active?.id === selected.id ? "primary" : "outline"} icon={ArrowRight} onClick={(): void => { selectWorkspace(selected.id); onOpenChat(); }}>Open in Chat</Button>
              </DetailActions>
            </Detail>
          ) : <WorkspaceEmptyState icon={FolderOpen} title="Select a workspace" description="Choose a project folder from the list or add a new one." />}
        </WorkspacePanel>
      </ProjectsGrid>
    </WorkspacePage>
  );
}
