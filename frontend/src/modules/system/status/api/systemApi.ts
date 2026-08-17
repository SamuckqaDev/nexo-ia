import { ApiError } from "../../../../shared/api/ApiError";
import { apiClient } from "../../../../shared/api/client";
import { systemBaseResponseSchema } from "../schemas/systemResponseSchema";
import type { SystemResponse } from "../types/systemTypes";

export function getSystemInformation(signal?: AbortSignal): Promise<SystemResponse> {
  return apiClient.get<unknown>("/system", { signal }).then((response) => {
    const result = systemBaseResponseSchema.safeParse(response.data);

    if (!result.success) {
      throw new ApiError(502, "Nexo returned an invalid system response", 502, result.error);
    }

    const [system] = result.data.data;

    if (!system) {
      throw new ApiError(502, "Nexo returned an empty system response", 502);
    }

    return system;
  });
}
