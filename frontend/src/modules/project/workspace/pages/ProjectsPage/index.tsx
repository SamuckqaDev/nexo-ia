import {
  ArrowRight,
  CheckCircle,
  FolderOpen,
  FolderSimplePlus,
  GitBranch,
  HardDrives,
  MagnifyingGlass,
  Plus,
  ShieldCheck,
  Trash
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { WorkspaceBadge, WorkspaceEmptyState, WorkspacePage, WorkspacePanel } from "../../../../../shared/components/WorkspacePage";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import { WorkspaceForm } from "../../components/WorkspaceForm";
import { WorkspaceTree } from "../../components/WorkspaceTree";
import { useActiveWorkspace } from "../../hooks/useActiveWorkspace";
import { useWorkspaceSnapshot } from "../../hooks/useWorkspaceSnapshot";
import { workspacePlatformLabel } from "../../services/workspacePlatformService";
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
  StorageWarning,
  Structure,
  StructureStatus,
  WorkspaceButton,
  WorkspaceCopy,
  WorkspaceList
} from "./styles";

export function ProjectsPage({ onOpenChat }: ProjectsPageProps): ReactElement {
  const workspaces: ProjectWorkspace[] = useWorkspaceStore((state: WorkspaceState) => state.workspaces);
  const selectWorkspace: WorkspaceState["selectWorkspace"] = useWorkspaceStore((state: WorkspaceState) => state.selectWorkspace);
  const forgetWorkspace: WorkspaceState["forgetWorkspace"] = useWorkspaceStore((state: WorkspaceState) => state.forgetWorkspace);
  const persistenceError: string | null = useWorkspaceStore((state: WorkspaceState) => state.persistenceError);
  const active = useActiveWorkspace();
  const [selectedId, setSelectedId] = useState<string | null>(active?.id ?? workspaces[0]?.id ?? null);
  const [adding, setAdding] = useState<boolean>(false);
  const [query, setQuery] = useState<string>("");
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);
  const selected: ProjectWorkspace | undefined = workspaces.find((workspace: ProjectWorkspace) => workspace.id === selectedId) ?? active;
  const workspaceSnapshot = useWorkspaceSnapshot(selected?.id ?? null);
  const visibleWorkspaces = useMemo<ProjectWorkspace[]>(() => workspaces.filter((workspace: ProjectWorkspace) =>
    `${workspace.name} ${workspace.directoryName}`.toLowerCase().includes(query.toLowerCase())
  ), [query, workspaces]);

  useEffect((): void => {
    if (!selectedId && (active ?? workspaces[0])) setSelectedId((active ?? workspaces[0])?.id ?? null);
  }, [active, selectedId, workspaces]);

  const chooseWorkspace = (workspace: ProjectWorkspace): void => {
    setSelectedId(workspace.id);
    selectWorkspace(workspace.id);
    setAdding(false);
  };

  const forgetSelectedWorkspace = (workspace: ProjectWorkspace): void => {
    ask({
      title: `Forget ${workspace.name}?`,
      message: "Nexo will remove the saved folder handle and structure snapshot from this browser. Files on your computer will not be changed.",
      confirmLabel: "Forget workspace",
      tone: "danger"
    }).then((confirmed: boolean): void => {
      if (!confirmed) return;
      forgetWorkspace(workspace.id).then((): void => {
        setSelectedId(null);
        setAdding(false);
      }).catch((): void => undefined);
    });
  };

  return (
    <WorkspacePage
      eyebrow="Project context"
      title="Projects & workspaces"
      description="Choose a real folder with your operating system's file explorer, save it on this device, and enter Chat or Cowork with the same active project context."
      icon={FolderOpen}
      actions={<Button type="button" icon={Plus} onClick={(): void => setAdding(true)}>Add workspace</Button>}
    >
      {active && (
        <ActiveContext>
          <FolderOpen size={22} weight="fill" />
          <div><span>Active workspace</span><strong>{active.name}</strong><small>{active.directoryName} · {workspacePlatformLabel(active.platform)}</small></div>
          <WorkspaceBadge tone="positive">Selected</WorkspaceBadge>
          <Button type="button" icon={ArrowRight} onClick={onOpenChat}>Open Chat</Button>
        </ActiveContext>
      )}

      {persistenceError && <StorageWarning role="alert">{persistenceError}</StorageWarning>}

      <ProjectsGrid>
        <WorkspacePanel title="Project folders" description="Switching here changes the saved workspace shown across Nexo." action={<WorkspaceBadge>{workspaces.length} saved on this device</WorkspaceBadge>}>
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
                      <span>{workspace.directoryName} · {workspacePlatformLabel(workspace.platform)}</span>
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
                description={workspaces.length ? "Try another project or folder name." : "Open your operating system's folder picker and select the project root Nexo should monitor. You can switch it later from the sidebar."}
                action={!workspaces.length ? <Button type="button" icon={FolderSimplePlus} onClick={(): void => setAdding(true)}>Choose first folder</Button> : undefined}
              />
            )}
          </Library>
        </WorkspacePanel>

        <WorkspacePanel as="aside" title={adding ? "Add a project folder" : selected ? "Workspace details" : "No workspace selected"} description={adding ? "Select the real folder from the file explorer and choose the initial session scope." : "Review the saved local context before starting."}>
          {adding ? (
            <WorkspaceForm onAdded={(workspace: ProjectWorkspace): void => { setSelectedId(workspace.id); setAdding(false); }} onCancel={(): void => setAdding(false)} />
          ) : selected ? (
            <Detail>
              <DetailHeader>
                <span><FolderOpen size={25} weight="duotone" /></span>
                <div><WorkspaceBadge tone={active?.id === selected.id ? "positive" : "default"}>{active?.id === selected.id ? "Active" : "Available"}</WorkspaceBadge><h2>{selected.name}</h2></div>
              </DetailHeader>
              <Path><span>Browser-authorized directory</span><code>{selected.directoryName}</code><small>The browser intentionally does not expose the absolute path to the web application.</small></Path>
              <DetailMeta>
                <div><HardDrives size={18} /><span>Session scope<strong>{selected.access}</strong></span></div>
                <div><GitBranch size={18} /><span>Git context<strong>{selected.branch ?? "Detect on connect"}</strong></span></div>
                <div><ShieldCheck size={18} /><span>Local access<strong>Browser managed · {workspacePlatformLabel(selected.platform)}</strong></span></div>
              </DetailMeta>
              <Structure>
                <div><strong>Workspace structure</strong><span>Saved metadata only; click a folder to expand it.</span></div>
                {workspaceSnapshot.status === "loading" && <StructureStatus role="status">Loading saved folder structure…</StructureStatus>}
                {workspaceSnapshot.status === "error" && <StructureStatus role="alert">Nexo could not read the saved structure from this browser.</StructureStatus>}
                {workspaceSnapshot.status === "ready" && workspaceSnapshot.snapshot && <WorkspaceTree snapshot={workspaceSnapshot.snapshot} />}
                {workspaceSnapshot.status === "ready" && !workspaceSnapshot.snapshot && <StructureStatus>No structure snapshot is available for this workspace.</StructureStatus>}
              </Structure>
              <DetailActions>
                <Button type="button" variant="outline" icon={Trash} onClick={(): void => forgetSelectedWorkspace(selected)}>Forget</Button>
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
