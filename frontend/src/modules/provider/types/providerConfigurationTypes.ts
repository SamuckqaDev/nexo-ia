import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { z } from "zod";

export const providerTypeSchema = z.enum(["OLLAMA", "OPENAI", "GOOGLE_GEMINI", "ANTHROPIC", "OPENAI_COMPATIBLE"]);
export type ProviderType = z.infer<typeof providerTypeSchema>;
export type ProviderConfiguration = { id: string; providerType: ProviderType; displayName: string; endpoint: string; selectedModel: string | null; enabled: boolean; lastConnectedAt: string | null };
export type ProviderConfigurationInput = { providerType: ProviderType; displayName: string; endpoint: string; selectedModel?: string };
export type ProviderRegistryResult = UseQueryResult<ProviderConfiguration[], Error>;
export type ProviderMutationResult = UseMutationResult<ProviderConfiguration, Error, ProviderConfigurationInput>;
export type ProviderUpdateInput = { id: string; input: ProviderConfigurationInput };
export type ProviderUpdateMutationResult = UseMutationResult<ProviderConfiguration, Error, ProviderUpdateInput>;
