import type { ChangeEvent } from "react";
import type { z } from "zod";
import type { createVaultSchema } from "../schemas/createVaultSchema";

export type VaultScope = "personal" | "project" | "team" | "organization";
export type VaultSourceStatus = "indexed" | "queued" | "local";

export type VaultSource = {
  id: string;
  name: string;
  type: string;
  size: string;
  status: VaultSourceStatus;
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
