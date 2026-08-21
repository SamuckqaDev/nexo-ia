import { useQuery } from "@tanstack/react-query";
import type { UseQueryResult } from "@tanstack/react-query";
import { listBackendKnowledgeGraph } from "../api/knowledgeGraphApi";
import type { BackendKnowledgeGraph } from "../types/vaultGraphTypes";

export const knowledgeGraphKey = ["knowledge", "graph"] as const;

export function useKnowledgeGraph(enabled: boolean): UseQueryResult<BackendKnowledgeGraph> {
  return useQuery({
    queryKey: knowledgeGraphKey,
    queryFn: listBackendKnowledgeGraph,
    enabled,
    retry: false
  });
}
