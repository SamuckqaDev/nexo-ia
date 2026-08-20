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

export const useVaultCatalogStore = create<VaultCatalogState>((set) => ({
  vaults: previewVaults,
  attachedSourceIds: [],
  createVault: (values: CreateVaultValues): KnowledgeVault => {
    const vault: KnowledgeVault = { id: crypto.randomUUID(), ...values, sources: [] };
    set((state: VaultCatalogState): Pick<VaultCatalogState, "vaults"> => ({ vaults: [vault, ...state.vaults] }));
    return vault;
  },
  addSources: (vaultId: string, sources: VaultSource[]): void => set((state: VaultCatalogState) => ({
    vaults: state.vaults.map((vault: KnowledgeVault): KnowledgeVault =>
      vault.id === vaultId ? { ...vault, sources: [...vault.sources, ...sources] } : vault)
  })),
  toggleSourceAttachment: (sourceId: string): void => set((state: VaultCatalogState) => ({
    attachedSourceIds: state.attachedSourceIds.includes(sourceId)
      ? state.attachedSourceIds.filter((id: string): boolean => id !== sourceId)
      : [...state.attachedSourceIds, sourceId]
  }))
}));

export function attachedVaultSources(state: VaultCatalogState): VaultSourceReference[] {
  return state.vaults.flatMap((vault: KnowledgeVault): VaultSourceReference[] =>
    vault.sources
      .filter((source: VaultSource): boolean =>
        state.attachedSourceIds.includes(source.id) && Boolean(source.contentPreview))
      .map((source: VaultSource): VaultSourceReference => ({ vaultId: vault.id, vaultName: vault.name, source }))
  );
}
