import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { knowledgeGraphSchema } from "../schemas/knowledgeGraphSchemas";
import type { BackendKnowledgeGraph } from "../types/vaultGraphTypes";

export const listBackendKnowledgeGraph = (): Promise<BackendKnowledgeGraph> =>
  apiClient.get<BaseResponse<unknown>>("/knowledge/graph")
    .then(({ data }) => {
      const graph: unknown = data.data?.[0];
      if (graph === undefined) throw new Error("Nexo IA returned an empty knowledge graph");
      return knowledgeGraphSchema.parse(graph);
    });
