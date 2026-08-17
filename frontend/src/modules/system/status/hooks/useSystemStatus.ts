import { useQuery } from "@tanstack/react-query";
import { getSystemInformation } from "../api/systemApi";
import type { SystemStatusResult } from "../types/systemTypes";

export function useSystemStatus(): SystemStatusResult {
  return useQuery({
    queryKey: ["system", "status"],
    queryFn: ({ signal }) => getSystemInformation(signal)
  });
}
