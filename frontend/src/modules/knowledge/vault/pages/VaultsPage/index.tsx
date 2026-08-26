import {
  ArrowRight,
  Buildings,
  File,
  FileArrowUp,
  Files,
  FolderOpen,
  MagnifyingGlass,
  Plus,
  ShareNetwork,
  Trash,
  Vault
} from "@phosphor-icons/react";
import { useEffect, useMemo, useRef, useState, type ChangeEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Loading } from "../../../../../shared/components/Loading";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import {
  WorkspaceBadge,
  WorkspaceEmptyState,
  WorkspacePage,
  WorkspacePanel,
  WorkspaceSegmentedControl
} from "../../../../../shared/components/WorkspacePage";
import { useTeams } from "../../../../organization/team/hooks/useTeams";
import type { Team } from "../../../../organization/team/types/teamTypes";
import { CreateVaultForm } from "../../components/CreateVaultForm";
import { VaultWorkbenchModal } from "../../components/VaultWorkbenchModal";
import { useBackendVaultCatalog } from "../../hooks/useBackendVaultCatalog";
import { useKnowledgeGraph } from "../../hooks/useKnowledgeGraph";
import { useVaultSources } from "../../hooks/useVaultSources";
import type { BackendSource, BackendVault, BackendVaultScope } from "../../types/backendVaultTypes";
import type { CreateVaultValues, VaultOwnerOption } from "../../types/vaultTypes";
import {
  Explorer,
  FileInput,
  Library,
  MetaGrid,
  MetaItem,
  PageActions,
  SourceAction,
  SourceList,
  SourceRow,
  Summary,
  VaultButton,
  VaultCopy,
  VaultIdentity,
  VaultList
} from "./styles";

type VaultFilter = "all" | "personal" | "team";

const STATUS_LABEL: Record<BackendSource["status"], { label: string; tone: "default" | "positive" | "attention" }> = {
  READY: { label: "Ready", tone: "positive" },
  INGESTING: { label: "Processing", tone: "attention" },
  REGISTERED: { label: "Queued", tone: "attention" },
  UNSUPPORTED: { label: "Not embedded", tone: "attention" },
  FAILED: { label: "Failed", tone: "attention" }
};

const formatBytes = (bytes: number): string =>
  bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(0)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;

export function VaultsPage(): ReactElement {
  const catalog = useBackendVaultCatalog();
  const teamsState = useTeams();
  const vaults: BackendVault[] = catalog.vaults.data ?? [];

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [query, setQuery] = useState<string>("");
  const [filter, setFilter] = useState<VaultFilter>("all");
  const [creating, setCreating] = useState<boolean>(false);
  const [workbenchOpen, setWorkbenchOpen] = useState<boolean>(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);

  const selected: BackendVault | undefined = vaults.find((vault: BackendVault) => vault.id === selectedId);
  const sourcesState = useVaultSources(selected?.id ?? null);
  const graphQuery = useKnowledgeGraph(workbenchOpen);
  const sources: BackendSource[] = sourcesState.sources.data ?? [];
  const readyCount: number = sources.filter((source: BackendSource) => source.status === "READY").length;

  const visibleVaults = useMemo<BackendVault[]>(
    () => vaults.filter((vault: BackendVault) => {
      const matchesOwner: boolean = filter === "all" || (filter === "team" ? vault.ownerType === "TEAM" : vault.ownerType === "USER");
      const needle: string = query.toLowerCase();
      return matchesOwner && `${vault.name} ${vault.ownerName}`.toLowerCase().includes(needle);
    }),
    [filter, query, vaults]
  );
  const ownerOptions = useMemo<VaultOwnerOption[]>((): VaultOwnerOption[] => [
    { label: "Personal space", value: "personal" },
    ...(teamsState.teams.data ?? [])
      .filter((team: Team) => team.manageable)
      .map((team: Team) => ({ label: `Team · ${team.name}`, value: `team:${team.id}` }))
  ], [teamsState.teams.data]);

  useEffect((): void => {
    if (creating || selected || !vaults.length) return;
    setSelectedId(vaults[0].id);
  }, [creating, selected, vaults]);

  const createVault = (values: CreateVaultValues): void => {
    const teamId: string | undefined = values.ownerTarget.startsWith("team:")
      ? values.ownerTarget.slice("team:".length)
      : undefined;
    const onSuccess = (vault: BackendVault): void => {
      setSelectedId(vault.id);
      setCreating(false);
    };
    if (teamId) {
      catalog.createTeam.mutate({
        teamId,
        name: values.name,
        description: values.description
      }, { onSuccess });
      return;
    }
    catalog.create.mutate({
        name: values.name,
        description: values.description || undefined,
        scope: values.scope.toUpperCase() as BackendVaultScope,
        workspaceId: values.workspaceId || undefined
    }, { onSuccess });
  };

  const addSources = (event: ChangeEvent<HTMLInputElement>): void => {
    const files: File[] = Array.from(event.target.files ?? []);
    files.forEach((file: File) => sourcesState.upload.mutate(file));
    event.target.value = "";
  };

  const removeVault = (vault: BackendVault): void => {
    ask({
      title: "Delete this Vault?",
      message: `"${vault.name}" and all of its embedded sources will be removed.`,
      confirmLabel: "Delete Vault",
      tone: "danger"
    }).then((confirmed: boolean): void => {
      if (!confirmed) return;
      catalog.archive.mutate(vault.id, { onSuccess: (): void => { if (vault.id === selectedId) setSelectedId(null); } });
    });
  };

  return (
    <>
      <WorkspacePage
      eyebrow="Grounded knowledge"
      title="Knowledge Vaults"
      description="Create governed knowledge collections and upload sources. Supported files are chunked and embedded into the vector store, ready for retrieval in Chat."
      icon={Vault}
      contentMode="contained"
      actions={(
        <PageActions>
          <Button type="button" variant="outline" size="compact" icon={ShareNetwork} onClick={(): void => setWorkbenchOpen(true)}>Knowledge graph</Button>
          <Button type="button" size="compact" icon={Plus} onClick={(): void => setCreating(true)}>New Vault</Button>
        </PageActions>
      )}
    >
      <Explorer>
        <WorkspacePanel title="Your Vaults" description="Personal and workspace collections stored in the backend.">
          <Library>
            <Input id="vault-search" label="Search Vaults" icon={MagnifyingGlass} value={query} onChange={(event): void => setQuery(event.target.value)} placeholder="Name" />
            <WorkspaceSegmentedControl
              label="Filter Vault ownership"
              value={filter}
              options={[{ label: "All", value: "all" }, { label: "Personal", value: "personal" }, { label: "Teams", value: "team" }]}
              onChange={setFilter}
            />
            {catalog.vaults.isLoading ? <Loading label="Loading your Vaults…" /> : visibleVaults.length ? (
              <VaultList>
                {visibleVaults.map((vault: BackendVault) => (
                  <VaultButton key={vault.id} type="button" $active={selectedId === vault.id} onClick={(): void => { setSelectedId(vault.id); setCreating(false); }}>
                    {vault.ownerType === "TEAM" ? <Buildings size={19} weight="duotone" /> : <FolderOpen size={19} weight="duotone" />}
                    <VaultCopy><strong>{vault.name}</strong><span>{vault.ownerName} · {vault.scope.toLowerCase()}</span></VaultCopy>
                    <ArrowRight size={14} />
                  </VaultButton>
                ))}
              </VaultList>
            ) : <WorkspaceEmptyState icon={Vault} title="No Vaults yet" description="Create your first Knowledge Vault to add sources." action={<Button type="button" icon={Plus} onClick={(): void => setCreating(true)}>New Vault</Button>} />}
          </Library>
        </WorkspacePanel>

        <WorkspacePanel
          title={creating ? "Create a Knowledge Vault" : selected?.name ?? "Select a Vault"}
          description={creating ? "Define ownership and purpose before adding sources." : selected?.description ?? undefined}
          action={!creating && selected ? (
            selected.manageable ? <Button type="button" variant="outline" size="compact" icon={Trash} onClick={(): void => removeVault(selected)}>Delete</Button> : <WorkspaceBadge>Read only</WorkspaceBadge>
          ) : undefined}
        >
          {creating ? (
            <CreateVaultForm
              ownerOptions={ownerOptions}
              pending={catalog.create.isPending || catalog.createTeam.isPending}
              onCreate={createVault}
              onCancel={(): void => setCreating(false)}
            />
          ) : selected ? (
            <>
              <Summary>
                <MetaGrid>
                  <MetaItem><span>Owner</span><strong>{selected.ownerName}</strong></MetaItem>
                  <MetaItem><span>Scope</span><strong>{selected.scope.toLowerCase()}</strong></MetaItem>
                  <MetaItem><span>Sources</span><strong>{sources.length}</strong></MetaItem>
                  <MetaItem><span>Embedded</span><strong>{readyCount} ready</strong></MetaItem>
                </MetaGrid>
                <SourceAction>
                  <FileInput ref={fileInput} type="file" multiple accept=".md,.txt,.json,.csv" onChange={addSources} />
                  <Button type="button" size="compact" icon={FileArrowUp} disabled={!selected.manageable || sourcesState.upload.isPending} onClick={(): void => fileInput.current?.click()}>
                    {selected.manageable ? (sourcesState.upload.isPending ? "Uploading…" : "Add sources") : "Team admin only"}
                  </Button>
                </SourceAction>
              </Summary>

              {sourcesState.sources.isLoading ? <Loading label="Loading sources…" /> : sources.length ? (
                <SourceList>
                  {sources.map((source: BackendSource) => (
                    <SourceRow key={source.id} type="button" $active={false} disabled={!selected.manageable} onClick={(): void => sourcesState.remove.mutate(source.id)} title={selected.manageable ? "Click to remove this source" : "Only a Team administrator can remove this source"}>
                      <File size={20} weight="duotone" />
                      <div><strong>{source.displayName}</strong><span>{source.mimeType} · {formatBytes(source.byteSize)}</span></div>
                      <WorkspaceBadge tone={STATUS_LABEL[source.status].tone}>{STATUS_LABEL[source.status].label}</WorkspaceBadge>
                    </SourceRow>
                  ))}
                </SourceList>
              ) : (
                <WorkspaceEmptyState
                  icon={Files}
                  title="This Vault has no sources"
                  description="Add Markdown, text, JSON or CSV files. They are chunked and embedded into the vector store so Chat can cite them. PDF and Office keep metadata only until a parser is connected."
                  action={selected.manageable ? <Button type="button" variant="outline" size="compact" icon={FileArrowUp} onClick={(): void => fileInput.current?.click()}>Choose files</Button> : <VaultIdentity><Buildings size={15} />Shared by {selected.ownerName}</VaultIdentity>}
                />
              )}
            </>
          ) : <WorkspaceEmptyState icon={Vault} title="Select a Vault" description="Choose a collection from the library or create a new one." />}
        </WorkspacePanel>
      </Explorer>
      </WorkspacePage>
      <VaultWorkbenchModal
        open={workbenchOpen}
        onClose={(): void => setWorkbenchOpen(false)}
        graphQuery={graphQuery}
        selectedVaultId={selectedId}
        onSelectVault={(vaultId: string): void => {
          setSelectedId(vaultId);
          setCreating(false);
        }}
      />
    </>
  );
}
