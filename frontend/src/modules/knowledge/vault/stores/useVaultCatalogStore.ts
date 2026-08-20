import { create } from "zustand";
import type {
  CreateVaultValues,
  KnowledgeVault,
  VaultCatalogState,
  VaultSource,
  VaultSourceReference
} from "../types/vaultTypes";

export const previewVaults: KnowledgeVault[] = [
  {
    id: "preview-docs",
    name: "Nexo product docs",
    description: "Architecture, product vision and governance references.",
    scope: "project",
    preview: true,
    sources: [
      {
        id: "source-vision",
        name: "PRODUCT_VISION.md",
        type: "Markdown",
        size: "12 KB",
        status: "preview",
        contentPreview: "Nexo IA is a local-first, team-ready AI workspace. Workspaces, Vaults, Skills and conversations remain separate scopes and require explicit authorization.",
        previewTruncated: true
      },
      {
        id: "source-governance",
        name: "CONTEXT_AND_SKILL_GOVERNANCE.md",
        type: "Markdown",
        size: "7 KB",
        status: "preview",
        contentPreview: "Explicit Skill invocation does not bypass identity, scope, policy or dependency checks. A Skill provides instructions, never permissions.",
        previewTruncated: true
      }
    ]
  },
  {
    id: "preview-research",
    name: "Personal research",
    description: "Private notes and reference documents.",
    scope: "personal",
    preview: true,
    sources: []
  }
];

type OwnerVaultCatalog = {
  vaults: KnowledgeVault[];
  attachedSourceIds: string[];
};

const catalogsByOwner = new Map<string, OwnerVaultCatalog>();

function cloneVaults(vaults: KnowledgeVault[]): KnowledgeVault[] {
  return vaults.map((vault: KnowledgeVault): KnowledgeVault => ({
    ...vault,
    sources: vault.sources.map((source: VaultSource): VaultSource => ({ ...source }))
  }));
}

function createOwnerCatalog(): OwnerVaultCatalog {
  return { vaults: cloneVaults(previewVaults), attachedSourceIds: [] };
}

function ownerCatalog(ownerId: string): OwnerVaultCatalog {
  const existing: OwnerVaultCatalog | undefined = catalogsByOwner.get(ownerId);
  if (existing) return existing;
  const created: OwnerVaultCatalog = createOwnerCatalog();
  catalogsByOwner.set(ownerId, created);
  return created;
}

function requireOwner(ownerId: string | null): string {
  if (!ownerId) throw new Error("A signed-in user is required to change Knowledge Vaults.");
  return ownerId;
}

export const useVaultCatalogStore = create<VaultCatalogState>((set, get) => ({
  ownerId: null,
  vaults: [],
  attachedSourceIds: [],
  initialize: (ownerId: string): void => {
    if (get().ownerId === ownerId) return;
    const catalog: OwnerVaultCatalog = ownerCatalog(ownerId);
    set({
      ownerId,
      vaults: cloneVaults(catalog.vaults),
      attachedSourceIds: [...catalog.attachedSourceIds]
    });
  },
  reset: (): void => set({ ownerId: null, vaults: [], attachedSourceIds: [] }),
  createVault: (values: CreateVaultValues): KnowledgeVault => {
    const ownerId: string = requireOwner(get().ownerId);
    const vault: KnowledgeVault = { id: crypto.randomUUID(), ...values, sources: [] };
    set((state: VaultCatalogState): Pick<VaultCatalogState, "vaults"> => {
      const vaults: KnowledgeVault[] = [vault, ...state.vaults];
      catalogsByOwner.set(ownerId, { vaults: cloneVaults(vaults), attachedSourceIds: [...state.attachedSourceIds] });
      return { vaults };
    });
    return vault;
  },
  addSources: (vaultId: string, sources: VaultSource[]): void => {
    const ownerId: string = requireOwner(get().ownerId);
    set((state: VaultCatalogState): Pick<VaultCatalogState, "vaults"> => {
      const vaults: KnowledgeVault[] = state.vaults.map((vault: KnowledgeVault): KnowledgeVault =>
        vault.id === vaultId ? { ...vault, sources: [...vault.sources, ...sources] } : vault);
      catalogsByOwner.set(ownerId, { vaults: cloneVaults(vaults), attachedSourceIds: [...state.attachedSourceIds] });
      return { vaults };
    });
  },
  toggleSourceAttachment: (sourceId: string): void => {
    const ownerId: string = requireOwner(get().ownerId);
    set((state: VaultCatalogState): Pick<VaultCatalogState, "attachedSourceIds"> => {
      const sourceBelongsToOwner: boolean = state.vaults.some((vault: KnowledgeVault): boolean =>
        vault.sources.some((source: VaultSource): boolean => source.id === sourceId));
      if (!sourceBelongsToOwner) return { attachedSourceIds: state.attachedSourceIds };
      const attachedSourceIds: string[] = state.attachedSourceIds.includes(sourceId)
        ? state.attachedSourceIds.filter((id: string): boolean => id !== sourceId)
        : [...state.attachedSourceIds, sourceId];
      catalogsByOwner.set(ownerId, { vaults: cloneVaults(state.vaults), attachedSourceIds: [...attachedSourceIds] });
      return { attachedSourceIds };
    });
  }
}));

export function attachedVaultSources(state: VaultCatalogState): VaultSourceReference[] {
  return state.vaults.flatMap((vault: KnowledgeVault): VaultSourceReference[] =>
    vault.sources
      .filter((source: VaultSource): boolean =>
        state.attachedSourceIds.includes(source.id) && Boolean(source.contentPreview))
      .map((source: VaultSource): VaultSourceReference => ({ vaultId: vault.id, vaultName: vault.name, source }))
  );
}
