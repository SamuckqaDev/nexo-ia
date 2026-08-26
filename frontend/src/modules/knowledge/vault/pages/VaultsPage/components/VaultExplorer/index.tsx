import {
  ArrowRight,
  Buildings,
  FolderOpen,
  MagnifyingGlass,
  Plus,
  Vault
} from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../../../shared/components/Button";
import { Input } from "../../../../../../../shared/components/Input";
import { Loading } from "../../../../../../../shared/components/Loading";
import {
  WorkspaceEmptyState,
  WorkspacePanel,
  WorkspaceSegmentedControl
} from "../../../../../../../shared/components/WorkspacePage";
import type { BackendVault } from "../../../../types/backendVaultTypes";
import type { VaultExplorerProps } from "../../../../types/vaultPageTypes";
import { Library, VaultButton, VaultCopy, VaultList } from "./styles";

export function VaultExplorer({
  vaults,
  selectedId,
  query,
  filter,
  loading,
  onQueryChange,
  onFilterChange,
  onSelect,
  onCreate
}: VaultExplorerProps): ReactElement {
  return (
    <WorkspacePanel title="Your Vaults" description="Personal and workspace collections stored in the backend.">
      <Library>
        <Input
          id="vault-search"
          label="Search Vaults"
          icon={MagnifyingGlass}
          value={query}
          onChange={(event): void => onQueryChange(event.target.value)}
          placeholder="Name"
        />
        <WorkspaceSegmentedControl
          label="Filter Vault ownership"
          value={filter}
          options={[
            { label: "All", value: "all" },
            { label: "Personal", value: "personal" },
            { label: "Teams", value: "team" }
          ]}
          onChange={onFilterChange}
        />
        {loading ? <Loading label="Loading your Vaults…" /> : vaults.length ? (
          <VaultList>
            {vaults.map((vault: BackendVault) => (
              <VaultButton
                key={vault.id}
                type="button"
                $active={selectedId === vault.id}
                $team={vault.ownerType === "TEAM"}
                onClick={(): void => onSelect(vault.id)}
              >
                {vault.ownerType === "TEAM"
                  ? <Buildings size={19} weight="duotone" />
                  : <FolderOpen size={19} weight="duotone" />}
                <VaultCopy $team={vault.ownerType === "TEAM"}>
                  <strong>{vault.name}</strong>
                  <span>{vault.ownerName} · {vault.scope.toLowerCase()}</span>
                </VaultCopy>
                <ArrowRight size={14} />
              </VaultButton>
            ))}
          </VaultList>
        ) : (
          <WorkspaceEmptyState
            icon={Vault}
            title="No Vaults yet"
            description="Create your first Knowledge Vault to add sources."
            action={<Button type="button" icon={Plus} onClick={onCreate}>New Vault</Button>}
          />
        )}
      </Library>
    </WorkspacePanel>
  );
}
