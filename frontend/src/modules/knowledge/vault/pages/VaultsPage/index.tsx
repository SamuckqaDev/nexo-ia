import {
  ArrowRight,
  CheckCircle,
  Eye,
  File,
  FileArrowUp,
  Files,
  FolderOpen,
  MagnifyingGlass,
  Paperclip,
  Plus,
  Vault
} from "@phosphor-icons/react";
import { useMemo, useRef, useState, type ChangeEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { WorkspaceBadge, WorkspaceEmptyState, WorkspacePage, WorkspacePanel } from "../../../../../shared/components/WorkspacePage";
import { CreateVaultForm } from "../../components/CreateVaultForm";
import { createVaultSource } from "../../services/vaultSourceService";
import { useVaultCatalogStore } from "../../stores/useVaultCatalogStore";
import type { CreateVaultValues, KnowledgeVault, VaultSource } from "../../types/vaultTypes";
import {
  Explorer,
  FileInput,
  Library,
  MetaGrid,
  MetaItem,
  SourceAction,
  SourceDetail,
  SourceDetailHeader,
  SourceList,
  SourcePreview,
  SourceRow,
  SourceStatus,
  Summary,
  VaultButton,
  VaultCopy,
  VaultList
} from "./styles";

export function VaultsPage(): ReactElement {
  const vaults = useVaultCatalogStore((state) => state.vaults);
  const createVaultDraft = useVaultCatalogStore((state) => state.createVault);
  const addVaultSources = useVaultCatalogStore((state) => state.addSources);
  const attachedSourceIds = useVaultCatalogStore((state) => state.attachedSourceIds);
  const toggleSourceAttachment = useVaultCatalogStore((state) => state.toggleSourceAttachment);
  const [selectedId, setSelectedId] = useState<string>(vaults[0]?.id ?? "");
  const [selectedSourceId, setSelectedSourceId] = useState<string | null>(vaults[0]?.sources[0]?.id ?? null);
  const [query, setQuery] = useState<string>("");
  const [creating, setCreating] = useState<boolean>(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const selected: KnowledgeVault | undefined = vaults.find((vault) => vault.id === selectedId);
  const selectedSource: VaultSource | undefined = selected?.sources.find((source: VaultSource): boolean => source.id === selectedSourceId);
  const visibleVaults = useMemo<KnowledgeVault[]>(() => vaults.filter((vault) => vault.name.toLowerCase().includes(query.toLowerCase())), [query, vaults]);

  const createVault = (values: CreateVaultValues): void => {
    const vault: KnowledgeVault = createVaultDraft(values);
    setSelectedId(vault.id);
    setSelectedSourceId(null);
    setCreating(false);
  };

  const addSources = (event: ChangeEvent<HTMLInputElement>): void => {
    const files: File[] = Array.from(event.target.files ?? []);
    if (!files.length || !selected) return;
    const vaultId: string = selected.id;
    Promise.all(files.map(createVaultSource)).then((additions: VaultSource[]): void => {
      addVaultSources(vaultId, additions);
      setSelectedSourceId(additions[0]?.id ?? null);
    });
    event.target.value = "";
  };

  return (
    <WorkspacePage
      eyebrow="Grounded knowledge"
      title="Knowledge Vaults"
      description="Create governed knowledge collections, inspect readable local sources, and explicitly attach bounded excerpts when Chat needs them."
      icon={Vault}
      actions={<Button type="button" icon={Plus} onClick={(): void => setCreating(true)}>New Vault</Button>}
    >
      <Explorer>
        <WorkspacePanel title="Your Vaults" description="Personal, project, team and organization collections.">
          <Library>
            <Input id="vault-search" label="Search Vaults" icon={MagnifyingGlass} value={query} onChange={(event): void => setQuery(event.target.value)} placeholder="Name or purpose" />
            <VaultList>
              {visibleVaults.map((vault) => (
                <VaultButton key={vault.id} type="button" $active={selectedId === vault.id} onClick={(): void => { setSelectedId(vault.id); setSelectedSourceId(vault.sources[0]?.id ?? null); setCreating(false); }}>
                  <FolderOpen size={19} weight="duotone" />
                  <VaultCopy><strong>{vault.name}</strong><span>{vault.sources.length} sources · {vault.scope}</span></VaultCopy>
                  <ArrowRight size={14} />
                </VaultButton>
              ))}
            </VaultList>
          </Library>
        </WorkspacePanel>

        <WorkspacePanel
          title={creating ? "Create a Knowledge Vault" : selected?.name}
          description={creating ? "Define ownership and purpose before adding sources." : selected?.description}
          action={!creating && selected ? <WorkspaceBadge tone={selected.preview ? "attention" : "default"}>{selected.preview ? "Preview collection" : "Session draft"}</WorkspaceBadge> : undefined}
        >
          {creating ? <CreateVaultForm onCreate={createVault} onCancel={(): void => setCreating(false)} /> : selected ? (
            <>
              <Summary>
                <MetaGrid>
                  <MetaItem><span>Scope</span><strong>{selected.scope}</strong></MetaItem>
                  <MetaItem><span>Sources</span><strong>{selected.sources.length}</strong></MetaItem>
                  <MetaItem><span>Chat context</span><strong>{selected.sources.filter((source: VaultSource): boolean => attachedSourceIds.includes(source.id)).length} attached</strong></MetaItem>
                </MetaGrid>
                <SourceAction>
                  <FileInput ref={fileInput} type="file" multiple accept=".md,.txt,.pdf,.doc,.docx,.json,.csv" onChange={addSources} />
                  <Button type="button" icon={FileArrowUp} onClick={(): void => fileInput.current?.click()}>Add sources</Button>
                </SourceAction>
              </Summary>
              {selected.sources.length ? (
                <SourceList>
                  {selected.sources.map((source) => (
                    <SourceRow key={source.id} type="button" $active={source.id === selectedSourceId} onClick={(): void => setSelectedSourceId(source.id)}>
                      <File size={20} weight="duotone" />
                      <div><strong>{source.name}</strong><span>{source.type} · {source.size}</span></div>
                      <SourceStatus $local={source.status === "local"}>{source.status === "local" ? <Files size={15} /> : <CheckCircle size={15} />}{source.status === "local" ? "Local session" : "Example"}</SourceStatus>
                    </SourceRow>
                  ))}
                </SourceList>
              ) : <WorkspaceEmptyState icon={Files} title="This Vault has no sources" description="Add Markdown, text, PDF, Office, JSON or CSV files. Supported text gets a session-only preview; other formats keep metadata until ingestion is connected." action={<Button type="button" variant="outline" icon={FileArrowUp} onClick={(): void => fileInput.current?.click()}>Choose files</Button>} />}
              {selectedSource && (
                <SourceDetail>
                  <SourceDetailHeader>
                    <div><Eye size={18} weight="duotone" /><span><strong>{selectedSource.name}</strong><small>Knowledge visible in this browser session</small></span></div>
                    <Button
                      type="button"
                      variant={attachedSourceIds.includes(selectedSource.id) ? "primary" : "outline"}
                      icon={Paperclip}
                      disabled={!selectedSource.contentPreview}
                      onClick={(): void => toggleSourceAttachment(selectedSource.id)}
                    >
                      {attachedSourceIds.includes(selectedSource.id) ? "Attached to Chat" : "Attach to Chat"}
                    </Button>
                  </SourceDetailHeader>
                  <SourcePreview>
                    {selectedSource.contentPreview
                      ? <>{selectedSource.contentPreview}{selectedSource.previewTruncated && <small>Preview truncated to keep local context bounded.</small>}</>
                      : <span>{selectedSource.previewUnavailableReason ?? "No readable content is available for this source."}</span>}
                  </SourcePreview>
                </SourceDetail>
              )}
            </>
          ) : <WorkspaceEmptyState icon={Vault} title="Select a Vault" description="Choose a collection from the library or create a new one." />}
        </WorkspacePanel>
      </Explorer>
    </WorkspacePage>
  );
}
