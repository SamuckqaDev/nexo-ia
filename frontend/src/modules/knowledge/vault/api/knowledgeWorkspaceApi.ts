import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { knowledgeWorkspaceSchema } from "../schemas/knowledgeWorkspaceSchema";
import type { KnowledgeWorkspace } from "../types/knowledgeWorkspaceTypes";

const first = <T>(response: BaseResponse<unknown>, parse: (value: unknown) => T): T => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty response");
  return parse(value);
};

export const listKnowledgeWorkspaces = (): Promise<KnowledgeWorkspace[]> =>
  apiClient.get<BaseResponse<unknown>>("/workspaces")
    .then(({ data }) => (data.data ?? []).map((item: unknown) => knowledgeWorkspaceSchema.parse(item)));

export const createKnowledgeWorkspace = (name: string): Promise<KnowledgeWorkspace> =>
  apiClient.post<BaseResponse<unknown>>("/workspaces", { name })
    .then(({ data }) => first(data, (value) => knowledgeWorkspaceSchema.parse(value)));
