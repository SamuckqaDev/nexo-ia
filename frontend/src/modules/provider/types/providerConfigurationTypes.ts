import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { z } from "zod";

export const providerTypeSchema = z.enum(["OLLAMA", "OPENAI", "GOOGLE_GEMINI", "ANTHROPIC", "OPENAI_COMPATIBLE"]);
export type ProviderType = z.infer<typeof providerTypeSchema>;
export type ProviderConfiguration = { id: string; providerType: ProviderType; displayName: string; endpoint: string; selectedModel: string | null; enabled: boolean; lastConnectedAt: string | null };
export type ProviderCatalogStatus = "AVAILABLE" | "EMPTY" | "UNAVAILABLE" | "UNSUPPORTED";
export type ProviderModel = {
  name: string;
  modifiedAt: string | null;
  size: number | null;
  toolCallingSupported: boolean | null;
};
export type ProviderModelCatalog = {
  providerConfigurationId: string;
  providerType: ProviderType;
  displayName: string;
  selectedModel: string | null;
  status: ProviderCatalogStatus;
  models: ProviderModel[];
  message: string | null;
};
export type ProviderModelCatalogView = Omit<ProviderModelCatalog, "status"> & {
  status: ProviderCatalogStatus | "LOADING";
};
export type ProviderConfigurationInput = { providerType: ProviderType; displayName: string; endpoint: string; selectedModel?: string };
export type ProviderConnectionTestInput = { providerType: ProviderType; endpoint: string };
export type ProviderConnectionTest = { endpoint: string; status: ProviderCatalogStatus; processingLocation: "LOCAL" | "REMOTE" | null; models: ProviderModel[]; message: string | null };
export type ProviderConnectionTestMutationResult = UseMutationResult<ProviderConnectionTest, Error, ProviderConnectionTestInput>;
export type ProviderRegistryResult = UseQueryResult<ProviderConfiguration[], Error>;
export type ProviderMutationResult = UseMutationResult<ProviderConfiguration, Error, ProviderConfigurationInput>;
export type ProviderUpdateInput = { id: string; input: ProviderConfigurationInput };
export type ProviderUpdateMutationResult = UseMutationResult<ProviderConfiguration, Error, ProviderUpdateInput>;
