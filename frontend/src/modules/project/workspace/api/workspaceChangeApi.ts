import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { workspaceChangeSchema } from "../schemas/workspaceChangeSchemas";
import type { WorkspaceChange } from "../types/workspaceChangeTypes";

const first = (response: BaseResponse<unknown>): WorkspaceChange => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty workspace-change response");
  return workspaceChangeSchema.parse(value);
};

export const listWorkspaceChanges = (conversationId: string): Promise<WorkspaceChange[]> =>
  apiClient.get<BaseResponse<unknown>>(`/workspace-changes/conversations/${conversationId}`)
    .then(({ data }) => (data.data ?? []).map((value: unknown) => workspaceChangeSchema.parse(value)));

export const approveWorkspaceChange = (changeId: string): Promise<WorkspaceChange> =>
  apiClient.post<BaseResponse<unknown>>(`/workspace-changes/${changeId}/approve`)
    .then(({ data }) => first(data));

export const denyWorkspaceChange = (changeId: string): Promise<WorkspaceChange> =>
  apiClient.post<BaseResponse<unknown>>(`/workspace-changes/${changeId}/deny`)
    .then(({ data }) => first(data));

export const revertWorkspaceChange = (changeId: string): Promise<WorkspaceChange> =>
  apiClient.post<BaseResponse<unknown>>(`/workspace-changes/${changeId}/revert`)
    .then(({ data }) => first(data));
