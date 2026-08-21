import {
  ArrowRight,
  File,
  FileArrowUp,
  Files,
  FolderOpen,
  MagnifyingGlass,
  Plus,
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
  WorkspacePanel
} from "../../../../../shared/components/WorkspacePage";
import { CreateVaultForm } from "../../components/CreateVaultForm";
import { useBackendVaultCatalog } from "../../hooks/useBackendVaultCatalog";
import { useVaultSources } from "../../hooks/useVaultSources";
import type { BackendSource, BackendVault, BackendVaultScope } from "../../types/backendVaultTypes";
import type { CreateVaultValues } from "../../types/vaultTypes";
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
  VaultList
} from "./styles";

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
  const vaults: BackendVault[] = catalog.vaults.data ?? [];

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [query, setQuery] = useState<string>("");
  const [creating, setCreating] = useState<boolean>(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);

  const selected: BackendVault | undefined = vaults.find((vault: BackendVault) => vault.id === selectedId);
  const sourcesState = useVaultSources(selected?.id ?? null);
  const sources: BackendSource[] = sourcesState.sources.data ?? [];
  const readyCount: number = sources.filter((source: BackendSource) => source.status === "READY").length;

  const visibleVaults = useMemo<BackendVault[]>(
    () => vaults.filter((vault: BackendVault) => vault.name.toLowerCase().includes(query.toLowerCase())),
    [query, vaults]
  );

  useEffect((): void => {
    if (creating || selected || !vaults.length) return;
    setSelectedId(vaults[0].id);
  }, [creating, selected, vaults]);

  const createVault = (values: CreateVaultValues): void => {
    catalog.create.mutate(
      {
        name: values.name,
        description: values.description || undefined,
        scope: values.scope.toUpperCase() as BackendVaultScope,
        workspaceId: values.workspaceId || undefined
      },
      {
        onSuccess: (vault: BackendVault): void => {
          setSelectedId(vault.id);
          setCreating(false);
        }
      }
    );
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
    <WorkspacePage
      eyebrow="Grounded knowledge"
      title="Knowledge Vaults"
      description="Create governed knowledge collections and upload sources. Supported files are chunked and embedded into the vector store, ready for retrieval in Chat."
      icon={Vault}
      contentMode="contained"
      actions={(
        <PageActions>
          <Button type="button" icon={Plus} onClick={(): void => setCreating(true)}>New Vault</Button>
        </PageActions>
      )}
    >
      <Explorer>
        <WorkspacePanel title="Your Vaults" description="Personal and workspace collections stored in the backend.">
          <Library>
            <Input id="vault-search" label="Search Vaults" icon={MagnifyingGlass} value={query} onChange={(event): void => setQuery(event.target.value)} placeholder="Name" />
            {catalog.vaults.isLoading ? <Loading label="Loading your Vaults…" /> : visibleVaults.length ? (
              <VaultList>
                {visibleVaults.map((vault: BackendVault) => (
                  <VaultButton key={vault.id} type="button" $active={selectedId === vault.id} onClick={(): void => { setSelectedId(vault.id); setCreating(false); }}>
                    <FolderOpen size={19} weight="duotone" />
                    <VaultCopy><strong>{vault.name}</strong><span>{vault.scope.toLowerCase()}</span></VaultCopy>
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
            <Button type="button" variant="outline" icon={Trash} onClick={(): void => removeVault(selected)}>Delete</Button>
          ) : undefined}
        >
          {creating ? (
            <CreateVaultForm onCreate={createVault} onCancel={(): void => setCreating(false)} />
          ) : selected ? (
            <>
              <Summary>
                <MetaGrid>
                  <MetaItem><span>Scope</span><strong>{selected.scope.toLowerCase()}</strong></MetaItem>
                  <MetaItem><span>Sources</span><strong>{sources.length}</strong></MetaItem>
                  <MetaItem><span>Embedded</span><strong>{readyCount} ready</strong></MetaItem>
                </MetaGrid>
                <SourceAction>
                  <FileInput ref={fileInput} type="file" multiple accept=".md,.txt,.json,.csv" onChange={addSources} />
                  <Button type="button" icon={FileArrowUp} disabled={sourcesState.upload.isPending} onClick={(): void => fileInput.current?.click()}>
                    {sourcesState.upload.isPending ? "Uploading…" : "Add sources"}
                  </Button>
                </SourceAction>
              </Summary>

              {sourcesState.sources.isLoading ? <Loading label="Loading sources…" /> : sources.length ? (
                <SourceList>
                  {sources.map((source: BackendSource) => (
                    <SourceRow key={source.id} type="button" $active={false} onClick={(): void => sourcesState.remove.mutate(source.id)} title="Click to remove this source">
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
                  action={<Button type="button" variant="outline" icon={FileArrowUp} onClick={(): void => fileInput.current?.click()}>Choose files</Button>}
                />
              )}
            </>
          ) : <WorkspaceEmptyState icon={Vault} title="Select a Vault" description="Choose a collection from the library or create a new one." />}
        </WorkspacePanel>
      </Explorer>
    </WorkspacePage>
  );
}
