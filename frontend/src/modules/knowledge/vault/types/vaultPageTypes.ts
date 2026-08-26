import type { BackendSource, BackendVault } from "./backendVaultTypes";
import type { CreateVaultValues, VaultOwnerOption } from "./vaultTypes";

export type VaultFilter = "all" | "personal" | "team";

export type VaultExplorerProps = {
  vaults: BackendVault[];
  selectedId: string | null;
  query: string;
  filter: VaultFilter;
  loading: boolean;
  onQueryChange: (query: string) => void;
  onFilterChange: (filter: VaultFilter) => void;
  onSelect: (vaultId: string) => void;
  onCreate: () => void;
};

export type VaultDetailsProps = {
  creating: boolean;
  selected?: BackendVault;
  sources: BackendSource[];
  ownerOptions: VaultOwnerOption[];
  createPending: boolean;
  sourcesLoading: boolean;
  uploadPending: boolean;
  onCreate: (values: CreateVaultValues) => void;
  onCancelCreate: () => void;
  onRemoveVault: (vault: BackendVault) => void;
  onUploadSources: (files: File[]) => void;
  onRemoveSource: (sourceId: string) => void;
};
