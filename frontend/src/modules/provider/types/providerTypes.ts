import type { UseQueryResult } from "@tanstack/react-query";

export type ProviderModel = {
  name: string;
  modifiedAt: string | null;
  size: number | null;
};

export type ProviderStatus = {
  id: string;
  name: string;
  kind: "LOCAL" | "REMOTE";
  endpoint: string;
  connected: boolean;
  models: ProviderModel[];
};

export type ProviderStatusResult = UseQueryResult<ProviderStatus, Error>;
