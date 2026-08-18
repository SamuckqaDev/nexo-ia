import { z } from "zod";
import { providerTypeSchema } from "../types/providerConfigurationTypes";

export const providerConfigurationSchema = z.object({
  providerType: providerTypeSchema,
  displayName: z.string().trim().min(2, "Enter a provider name").max(100),
  endpoint: z.url("Enter a valid provider URL").max(500),
  selectedModel: z.string().trim().max(160)
});
export type ProviderConfigurationFormValues = z.infer<typeof providerConfigurationSchema>;
