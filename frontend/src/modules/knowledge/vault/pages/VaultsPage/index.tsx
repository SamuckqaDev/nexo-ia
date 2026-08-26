import { Plus, ShareNetwork, Vault } from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { WorkspacePage } from "../../../../../shared/components/WorkspacePage";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import { useTeams } from "../../../../organization/team/hooks/useTeams";
import type { Team } from "../../../../organization/team/types/teamTypes";
import { VaultWorkbenchModal } from "../../components/VaultWorkbenchModal";
import { useBackendVaultCatalog } from "../../hooks/useBackendVaultCatalog";
import { useKnowledgeGraph } from "../../hooks/useKnowledgeGraph";
import { useVaultSources } from "../../hooks/useVaultSources";
import type { BackendSource, BackendVault, BackendVaultScope } from "../../types/backendVaultTypes";
import type { VaultFilter } from "../../types/vaultPageTypes";
import type { CreateVaultValues, VaultOwnerOption } from "../../types/vaultTypes";
import { VaultDetails } from "./components/VaultDetails";
import { VaultExplorer } from "./components/VaultExplorer";
import { Explorer, PageActions } from "./styles";

export function VaultsPage(): ReactElement {
  const catalog = useBackendVaultCatalog();
  const teamsState = useTeams();
  const vaults: BackendVault[] = catalog.vaults.data ?? [];

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [query, setQuery] = useState<string>("");
  const [filter, setFilter] = useState<VaultFilter>("all");
  const [creating, setCreating] = useState<boolean>(false);
  const [workbenchOpen, setWorkbenchOpen] = useState<boolean>(false);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);

  const selected: BackendVault | undefined = vaults.find((vault: BackendVault) => vault.id === selectedId);
  const sourcesState = useVaultSources(selected?.id ?? null);
  const graphQuery = useKnowledgeGraph(workbenchOpen);
  const sources: BackendSource[] = sourcesState.sources.data ?? [];

  const visibleVaults = useMemo<BackendVault[]>(
    () => vaults.filter((vault: BackendVault) => {
      const matchesOwner: boolean = filter === "all"
        || (filter === "team" ? vault.ownerType === "TEAM" : vault.ownerType === "USER");
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

  const selectVault = (vaultId: string): void => {
    setSelectedId(vaultId);
    setCreating(false);
  };
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
  const uploadSources = (files: File[]): void => {
    files.forEach((file: File) => sourcesState.upload.mutate(file));
  };
  const removeVault = (vault: BackendVault): void => {
    ask({
      title: "Delete this Vault?",
      message: `"${vault.name}" and all of its embedded sources will be removed.`,
      confirmLabel: "Delete Vault",
      tone: "danger"
    }).then((confirmed: boolean): void => {
      if (!confirmed) return;
      catalog.archive.mutate(vault.id, {
        onSuccess: (): void => {
          if (vault.id === selectedId) setSelectedId(null);
        }
      });
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
          <VaultExplorer
            vaults={visibleVaults}
            selectedId={selectedId}
            query={query}
            filter={filter}
            loading={catalog.vaults.isLoading}
            onQueryChange={setQuery}
            onFilterChange={setFilter}
            onSelect={selectVault}
            onCreate={(): void => setCreating(true)}
          />
          <VaultDetails
            creating={creating}
            selected={selected}
            sources={sources}
            ownerOptions={ownerOptions}
            createPending={catalog.create.isPending || catalog.createTeam.isPending}
            sourcesLoading={sourcesState.sources.isLoading}
            uploadPending={sourcesState.upload.isPending}
            onCreate={createVault}
            onCancelCreate={(): void => setCreating(false)}
            onRemoveVault={removeVault}
            onUploadSources={uploadSources}
            onRemoveSource={(sourceId: string): void => sourcesState.remove.mutate(sourceId)}
          />
        </Explorer>
      </WorkspacePage>
      <VaultWorkbenchModal
        open={workbenchOpen}
        onClose={(): void => setWorkbenchOpen(false)}
        graphQuery={graphQuery}
        selectedVaultId={selectedId}
        onSelectVault={selectVault}
      />
    </>
  );
}
