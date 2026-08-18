import { apiClient } from "../../../shared/api/client";
import type { BaseResponse } from "../../../shared/types/apiTypes";
import { usageSummarySchema } from "../schemas/usageSchemas";
import type { UsagePeriod, UsageSummary } from "../types/usageTypes";

export const fetchUsageSummary = (period: UsagePeriod): Promise<UsageSummary> =>
  apiClient.get<BaseResponse<unknown>>("/usage", { params: { period } })
    .then(({ data }) => {
      const value: unknown = data.data?.[0];
      if (value === undefined) throw new Error("Nexo IA returned an empty usage response");

      return usageSummarySchema.parse(value);
    });
