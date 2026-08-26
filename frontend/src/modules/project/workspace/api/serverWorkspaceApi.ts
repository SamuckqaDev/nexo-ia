import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import {
  serverWorkspaceSchema,
  serverWorkspaceStatusSchema,
  serverWorkspaceTreeSchema
} from "../schemas/serverWorkspaceSchemas";
import type {
  CreateServerWorkspaceInput,
  ServerWorkspace,
  ServerWorkspaceStatus,
  ServerWorkspaceTree
} from "../types/serverWorkspaceTypes";

const first = <T>(response: BaseResponse<unknown>, parse: (value: unknown) => T): T => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty workspace response");
  return parse(value);
};

export const listServerWorkspaces = (): Promise<ServerWorkspace[]> =>
  apiClient.get<BaseResponse<unknown>>("/workspaces")
    .then(({ data }) => (data.data ?? []).map((item: unknown) => serverWorkspaceSchema.parse(item)));

export const createServerWorkspace = (input: CreateServerWorkspaceInput): Promise<ServerWorkspace> =>
  apiClient.post<BaseResponse<unknown>>("/workspaces", { name: input.name.trim() })
    .then(({ data }) => first(data, (value) => serverWorkspaceSchema.parse(value)))
    .then((created: ServerWorkspace) => apiClient.put<BaseResponse<unknown>>(
      `/workspaces/${created.id}/binding`,
      {
        storageType: input.storageType,
        accessMode: input.accessMode,
        relativePath: input.storageType === "MOUNTED" ? input.relativePath?.trim() : null
      }
    ).then(({ data }) => first(data, (value) => serverWorkspaceSchema.parse(value))));

export const deleteServerWorkspace = (workspaceId: string): Promise<void> =>
  apiClient.delete<BaseResponse<unknown>>(`/workspaces/${workspaceId}`).then(() => undefined);

export const getServerWorkspaceStatus = (workspaceId: string): Promise<ServerWorkspaceStatus> =>
  apiClient.get<BaseResponse<unknown>>(`/workspaces/${workspaceId}/status`)
    .then(({ data }) => first(data, (value) => serverWorkspaceStatusSchema.parse(value)));

export const refreshServerWorkspace = (workspaceId: string): Promise<ServerWorkspaceStatus> =>
  apiClient.post<BaseResponse<unknown>>(`/workspaces/${workspaceId}/refresh`)
    .then(({ data }) => first(data, (value) => serverWorkspaceStatusSchema.parse(value)));

export const getServerWorkspaceTree = (
  workspaceId: string,
  path = "",
  cursor?: string
): Promise<ServerWorkspaceTree> =>
  apiClient.get<BaseResponse<unknown>>(`/workspaces/${workspaceId}/tree`, {
    params: { ...(path ? { path } : {}), ...(cursor ? { cursor } : {}), limit: 200 }
  }).then(({ data }) => first(data, (value) => serverWorkspaceTreeSchema.parse(value)));
