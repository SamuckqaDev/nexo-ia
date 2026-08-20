import {
  ArrowRight,
  CheckCircle,
  File,
  FileArrowUp,
  Files,
  FolderOpen,
  MagnifyingGlass,
  Plus,
  Vault
} from "@phosphor-icons/react";
import { useMemo, useRef, useState, type ChangeEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { WorkspaceBadge, WorkspaceEmptyState, WorkspacePage, WorkspacePanel } from "../../../../../shared/components/WorkspacePage";
import { CreateVaultForm } from "../../components/CreateVaultForm";
import type { CreateVaultValues, KnowledgeVault, VaultSource } from "../../types/vaultTypes";
import {
  Explorer,
  FileInput,
  Library,
  MetaGrid,
  MetaItem,
  SourceAction,
  SourceList,
  SourceRow,
  SourceStatus,
  Summary,
  VaultButton,
  VaultCopy,
  VaultList
} from "./styles";

const formatSize = (bytes: number): string => bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
const previewVaults: KnowledgeVault[] = [
  {
    id: "preview-docs", name: "Nexo product docs", description: "Architecture, product vision and governance references.", scope: "project", preview: true,
    sources: [
      { id: "source-vision", name: "PRODUCT_VISION.md", type: "Markdown", size: "12 KB", status: "indexed" },
      { id: "source-governance", name: "CONTEXT_AND_SKILL_GOVERNANCE.md", type: "Markdown", size: "7 KB", status: "indexed" }
    ]
  },
  { id: "preview-research", name: "Personal research", description: "Private notes and reference documents.", scope: "personal", preview: true, sources: [] }
];

export function VaultsPage(): ReactElement {
  const [vaults, setVaults] = useState<KnowledgeVault[]>(previewVaults);
  const [selectedId, setSelectedId] = useState<string>(previewVaults[0].id);
  const [query, setQuery] = useState<string>("");
  const [creating, setCreating] = useState<boolean>(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const selected: KnowledgeVault | undefined = vaults.find((vault) => vault.id === selectedId);
  const visibleVaults = useMemo<KnowledgeVault[]>(() => vaults.filter((vault) => vault.name.toLowerCase().includes(query.toLowerCase())), [query, vaults]);

  const createVault = (values: CreateVaultValues): void => {
    const vault: KnowledgeVault = { id: crypto.randomUUID(), ...values, sources: [] };
    setVaults((current) => [vault, ...current]);
    setSelectedId(vault.id);
    setCreating(false);
  };

  const addSources = (event: ChangeEvent<HTMLInputElement>): void => {
    const additions: VaultSource[] = Array.from(event.target.files ?? []).map((file) => ({
      id: crypto.randomUUID(), name: file.name, type: file.type || "Document", size: formatSize(file.size), status: "local"
    }));
    if (!additions.length || !selected) return;
    setVaults((current) => current.map((vault) => vault.id === selected.id ? { ...vault, sources: [...vault.sources, ...additions] } : vault));
    event.target.value = "";
  };

  return (
    <WorkspacePage
      eyebrow="Grounded knowledge"
      title="Knowledge Vaults"
      description="Create governed knowledge collections, add portable sources and inspect exactly what Nexo may retrieve before a conversation uses it."
      icon={Vault}
      actions={<Button type="button" icon={Plus} onClick={(): void => setCreating(true)}>New Vault</Button>}
    >
      <Explorer>
        <WorkspacePanel title="Your Vaults" description="Personal, project, team and organization collections.">
          <Library>
            <Input id="vault-search" label="Search Vaults" icon={MagnifyingGlass} value={query} onChange={(event): void => setQuery(event.target.value)} placeholder="Name or purpose" />
            <VaultList>
              {visibleVaults.map((vault) => (
                <VaultButton key={vault.id} type="button" $active={selectedId === vault.id} onClick={(): void => { setSelectedId(vault.id); setCreating(false); }}>
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
                  <MetaItem><span>Retrieval</span><strong>{selected.preview ? "Preview only" : "Not connected"}</strong></MetaItem>
                </MetaGrid>
                <SourceAction>
                  <FileInput ref={fileInput} type="file" multiple accept=".md,.txt,.pdf,.doc,.docx,.json,.csv" onChange={addSources} />
                  <Button type="button" icon={FileArrowUp} onClick={(): void => fileInput.current?.click()}>Add sources</Button>
                </SourceAction>
              </Summary>
              {selected.sources.length ? (
                <SourceList>
                  {selected.sources.map((source) => (
                    <SourceRow key={source.id}>
                      <File size={20} weight="duotone" />
                      <div><strong>{source.name}</strong><span>{source.type} · {source.size}</span></div>
                      <SourceStatus $local={source.status === "local"}>{source.status === "local" ? <Files size={15} /> : <CheckCircle size={15} />}{source.status === "local" ? "Local draft" : source.status}</SourceStatus>
                    </SourceRow>
                  ))}
                </SourceList>
              ) : <WorkspaceEmptyState icon={Files} title="This Vault has no sources" description="Add Markdown, text, PDF, Office, JSON or CSV files. Files selected now remain local metadata until the knowledge API is connected." action={<Button type="button" variant="outline" icon={FileArrowUp} onClick={(): void => fileInput.current?.click()}>Choose files</Button>} />}
            </>
          ) : <WorkspaceEmptyState icon={Vault} title="Select a Vault" description="Choose a collection from the library or create a new one." />}
        </WorkspacePanel>
      </Explorer>
    </WorkspacePage>
  );
}
