import type { ChangeEvent } from "react";
import type { z } from "zod";
import type { createVaultSchema } from "../schemas/createVaultSchema";

export type VaultScope = "personal" | "project" | "team" | "organization";
export type VaultSourceStatus = "preview" | "queued" | "local";

export type VaultSource = {
  id: string;
  name: string;
  type: string;
  size: string;
  status: VaultSourceStatus;
  contentPreview?: string;
  previewTruncated?: boolean;
  previewUnavailableReason?: string;
};

export type KnowledgeVault = {
  id: string;
  name: string;
  description: string;
  scope: VaultScope;
  sources: VaultSource[];
  preview?: boolean;
};

export type CreateVaultValues = z.infer<typeof createVaultSchema>;
export type CreateVaultFormProps = { onCreate: (values: CreateVaultValues) => void; onCancel: () => void };
export type SourcePickerProps = { onSelect: (event: ChangeEvent<HTMLInputElement>) => void };

export type VaultSourceReference = {
  vaultId: string;
  vaultName: string;
  source: VaultSource;
};

export type VaultCatalogState = {
  vaults: KnowledgeVault[];
  attachedSourceIds: string[];
  createVault: (values: CreateVaultValues) => KnowledgeVault;
  addSources: (vaultId: string, sources: VaultSource[]) => void;
  toggleSourceAttachment: (sourceId: string) => void;
};
