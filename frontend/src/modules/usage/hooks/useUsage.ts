import { useQuery } from "@tanstack/react-query";
import type { UseQueryResult } from "@tanstack/react-query";
import { fetchUsageSummary } from "../api/usageApi";
import type { UsagePeriod, UsageSummary } from "../types/usageTypes";

export const useUsage = (period: UsagePeriod): UseQueryResult<UsageSummary> =>
  useQuery({
    queryKey: ["usage", period],
    queryFn: (): Promise<UsageSummary> => fetchUsageSummary(period)
  });
