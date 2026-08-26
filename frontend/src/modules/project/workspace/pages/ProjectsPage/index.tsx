import {
  ArrowClockwise,
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
import { useEffect, useMemo, useState, type FormEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { WorkspaceBadge, WorkspaceEmptyState, WorkspacePage, WorkspacePanel } from "../../../../../shared/components/WorkspacePage";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import { ServerWorkspaceTree } from "../../components/ServerWorkspaceTree";
import {
  useCreateServerWorkspace,
  useDeleteServerWorkspace,
  useRefreshServerWorkspace,
  useServerWorkspaces,
  useServerWorkspaceStatus
} from "../../hooks/useServerWorkspaces";
import type {
  ServerWorkspace,
  ServerWorkspaceAccessMode,
  ServerWorkspaceStorageType
} from "../../types/serverWorkspaceTypes";
import type { ProjectsPageProps } from "../../types/workspaceTypes";
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
  StructureHeader,
  StructureStatus,
  WorkspaceButton,
  WorkspaceCopy,
  WorkspaceList
} from "./styles";

export function ProjectsPage({ onOpenChat }: ProjectsPageProps): ReactElement {
  const workspaces = useServerWorkspaces();
  const create = useCreateServerWorkspace();
  const remove = useDeleteServerWorkspace();
  const refresh = useRefreshServerWorkspace();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [adding, setAdding] = useState<boolean>(false);
  const [query, setQuery] = useState<string>("");
  const [name, setName] = useState<string>("");
  const [relativePath, setRelativePath] = useState<string>("");
  const [storageType, setStorageType] = useState<Exclude<ServerWorkspaceStorageType, "UNBOUND">>("MANAGED");
  const [accessMode, setAccessMode] = useState<ServerWorkspaceAccessMode>("READ_ONLY");
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);
  const selected: ServerWorkspace | undefined = workspaces.data
    ?.find((workspace: ServerWorkspace) => workspace.id === selectedId);
  const status = useServerWorkspaceStatus(selected?.id ?? null);
  const visibleWorkspaces = useMemo<ServerWorkspace[]>(() => (workspaces.data ?? []).filter(
    (workspace: ServerWorkspace): boolean =>
      `${workspace.name} ${workspace.relativePath ?? ""}`.toLowerCase().includes(query.toLowerCase())
  ), [query, workspaces.data]);

  useEffect((): void => {
    if (!selectedId && workspaces.data?.[0]) setSelectedId(workspaces.data[0].id);
  }, [selectedId, workspaces.data]);

  const submit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    if (!name.trim() || (storageType === "MOUNTED" && !relativePath.trim())) return;
    create.mutate({ name, storageType, accessMode, relativePath }, {
      onSuccess: (workspace: ServerWorkspace): void => {
        setSelectedId(workspace.id);
        setAdding(false);
        setName("");
        setRelativePath("");
      }
    });
  };

  const deleteWorkspace = (workspace: ServerWorkspace): void => {
    ask({
      title: `Delete ${workspace.name}?`,
      message: "Nexo will remove this server workspace registration. Project files are never deleted by this action.",
      confirmLabel: "Delete workspace",
      tone: "danger"
    }).then((confirmed: boolean): void => {
      if (!confirmed) return;
      remove.mutate(workspace.id, { onSuccess: (): void => setSelectedId(null) });
    });
  };

  return (
    <WorkspacePage
      eyebrow="Server project context"
      title="Projects & workspaces"
      description="Register projects the Nexo server can inspect. Conversations select one persisted workspace, so Agent tools work from any browser without relying on a local folder handle."
      icon={FolderOpen}
      actions={<Button type="button" icon={Plus} onClick={(): void => setAdding(true)}>Add workspace</Button>}
    >
      {selected && (
        <ActiveContext>
          <FolderOpen size={22} weight="fill" />
          <div>
            <span>Selected server workspace</span>
            <strong>{selected.name}</strong>
            <small>{selected.storageType.toLowerCase()} · {selected.status.toLowerCase()}</small>
          </div>
          <WorkspaceBadge tone={selected.status === "AVAILABLE" ? "positive" : "default"}>{selected.status}</WorkspaceBadge>
          <Button type="button" icon={ArrowRight} onClick={onOpenChat}>Open Chat</Button>
        </ActiveContext>
      )}

      {(workspaces.isError || create.isError || remove.isError) && (
        <StorageWarning role="alert">
          {create.error?.message ?? remove.error?.message ?? "Nexo could not load the server workspace catalog."}
        </StorageWarning>
      )}

      <ProjectsGrid>
        <WorkspacePanel
          title="Server projects"
          description="Each registration belongs to your authenticated Nexo account."
          action={<WorkspaceBadge>{workspaces.data?.length ?? 0} registered</WorkspaceBadge>}
        >
          <Library>
            {(workspaces.data?.length ?? 0) > 0 && (
              <Input
                id="workspace-search"
                label="Find a workspace"
                icon={MagnifyingGlass}
                value={query}
                onChange={(event): void => setQuery(event.target.value)}
                placeholder="Name or relative import path"
              />
            )}
            {visibleWorkspaces.length > 0 ? (
              <WorkspaceList>
                {visibleWorkspaces.map((workspace: ServerWorkspace) => (
                  <WorkspaceButton
                    key={workspace.id}
                    type="button"
                    $active={selected?.id === workspace.id}
                    onClick={(): void => { setSelectedId(workspace.id); setAdding(false); }}
                  >
                    <FolderOpen size={21} weight={selected?.id === workspace.id ? "fill" : "duotone"} />
                    <WorkspaceCopy>
                      <strong>{workspace.name}</strong>
                      <span>{workspace.storageType.toLowerCase()}{workspace.relativePath ? ` · ${workspace.relativePath}` : ""}</span>
                      <small>{workspace.status.toLowerCase()}</small>
                    </WorkspaceCopy>
                    {selected?.id === workspace.id ? <CheckCircle size={18} weight="fill" /> : <ArrowRight size={16} />}
                  </WorkspaceButton>
                ))}
              </WorkspaceList>
            ) : (
              <WorkspaceEmptyState
                icon={FolderSimplePlus}
                title={workspaces.data?.length ? "No matching workspace" : "Register a server project"}
                description="Managed workspaces live under Nexo storage. Mounted workspaces resolve only below the server import root configured by the administrator."
                action={!workspaces.data?.length
                  ? <Button type="button" icon={FolderSimplePlus} onClick={(): void => setAdding(true)}>Add first workspace</Button>
                  : undefined}
              />
            )}
          </Library>
        </WorkspacePanel>

        <WorkspacePanel
          as="aside"
          title={adding ? "Add server workspace" : selected ? "Workspace details" : "No workspace selected"}
          description={adding
            ? "Choose Nexo-managed storage or a path relative to the configured server import root."
            : "Live status and structure come from the Nexo server, not browser storage."}
        >
          {adding ? (
            <Detail as="form" onSubmit={submit}>
              <Input id="server-workspace-name" label="Workspace name" value={name} maxLength={160} onChange={(event): void => setName(event.target.value)} placeholder="Nexo backend" />
              <Select
                id="server-workspace-storage"
                label="Storage"
                value={storageType}
                onChange={(event): void => setStorageType(event.target.value as Exclude<ServerWorkspaceStorageType, "UNBOUND">)}
                options={[
                  { label: "Managed by Nexo server", value: "MANAGED" },
                  { label: "Mounted below server import root", value: "MOUNTED" }
                ]}
              />
              {storageType === "MOUNTED" && (
                <Input id="server-workspace-path" label="Relative import path" value={relativePath} maxLength={1024} onChange={(event): void => setRelativePath(event.target.value)} placeholder="projects/nexo-ia" />
              )}
              <Select
                id="server-workspace-access"
                label="Governed access ceiling"
                value={accessMode}
                onChange={(event): void => setAccessMode(event.target.value as ServerWorkspaceAccessMode)}
                options={[
                  { label: "Read-only inspection", value: "READ_ONLY" },
                  { label: "Writes require approval", value: "WRITE_WITH_APPROVAL" },
                  { label: "Commands require approval", value: "COMMANDS_WITH_APPROVAL" }
                ]}
              />
              {create.isError && <StorageWarning role="alert">{create.error.message}</StorageWarning>}
              <DetailActions>
                <Button type="button" variant="outline" onClick={(): void => setAdding(false)}>Cancel</Button>
                <Button type="submit" disabled={create.isPending || !name.trim() || (storageType === "MOUNTED" && !relativePath.trim())}>
                  {create.isPending ? "Registering…" : "Register workspace"}
                </Button>
              </DetailActions>
            </Detail>
          ) : selected ? (
            <Detail>
              <DetailHeader>
                <span><FolderOpen size={25} weight="duotone" /></span>
                <div><WorkspaceBadge tone={status.data?.status === "AVAILABLE" ? "positive" : "default"}>{status.data?.status ?? selected.status}</WorkspaceBadge><h2>{selected.name}</h2></div>
              </DetailHeader>
              <Path>
                <span>Server binding</span>
                <code>{selected.storageType === "MANAGED" ? "Nexo managed storage" : selected.relativePath}</code>
                <small>Absolute paths never leave the backend. Tools receive only workspace-relative paths.</small>
              </Path>
              <DetailMeta>
                <div><HardDrives size={18} /><span>Storage<strong>{selected.storageType.toLowerCase()}</strong></span></div>
                <div><GitBranch size={18} /><span>Git context<strong>{status.data?.git?.branch ?? "Not detected"}</strong></span></div>
                <div><ShieldCheck size={18} /><span>Access ceiling<strong>{selected.accessMode.toLowerCase()}</strong></span></div>
              </DetailMeta>
              <Structure>
                <StructureHeader>
                  <div><strong>Live server structure</strong><span>Folders load lazily from the authenticated workspace endpoint.</span></div>
                  <Button type="button" variant="outline" icon={ArrowClockwise} disabled={refresh.isPending} onClick={(): void => refresh.mutate(selected.id)}>Refresh</Button>
                </StructureHeader>
                {status.isLoading && <StructureStatus>Checking server workspace…</StructureStatus>}
                {status.isError && <StructureStatus role="alert">Workspace inspection failed.</StructureStatus>}
                {status.data?.reason && <StructureStatus>{status.data.reason}</StructureStatus>}
                {(status.data?.status === "AVAILABLE" || status.data?.status === "CHANGED")
                  && <ServerWorkspaceTree workspaceId={selected.id} />}
              </Structure>
              <DetailActions>
                <Button type="button" variant="outline" icon={Trash} disabled={remove.isPending} onClick={(): void => deleteWorkspace(selected)}>Delete registration</Button>
                <Button type="button" icon={ArrowRight} onClick={onOpenChat}>Select in Chat</Button>
              </DetailActions>
            </Detail>
          ) : (
            <WorkspaceEmptyState icon={FolderOpen} title="Select a workspace" description="Choose a server workspace from the list or register a new one." />
          )}
        </WorkspacePanel>
      </ProjectsGrid>
    </WorkspacePage>
  );
}
