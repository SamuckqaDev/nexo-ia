import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { mcpCatalogSchema, mcpConnectionSchema } from "../schemas/mcpSchemas";
import type {
  McpCatalog,
  McpConnection,
  McpConnectionStateInput,
  McpToolSelectionInput,
  RemoteMcpConnectionInput
} from "../types/mcpTypes";

const first = <T>(response: BaseResponse<T>): T => {
  const value: T | undefined = response.data?.[0];
  if (!value) throw new Error("Nexo returned an empty MCP response");
  return value;
};

const parseConnection = (value: unknown): McpConnection => {
  const parsed = mcpConnectionSchema.safeParse(value);
  if (!parsed.success) {
    throw new Error("Nexo received an incompatible MCP connection response. Restart the updated backend and try again.");
  }
  return parsed.data;
};

export function getMcpCatalog(): Promise<McpCatalog> {
  return apiClient.get<BaseResponse<unknown>>("/mcp/catalog")
    .then(({ data }) => mcpCatalogSchema.parse(first(data)));
}

export function listMcpConnections(): Promise<McpConnection[]> {
  return apiClient.get<BaseResponse<unknown>>("/mcp/connections")
    .then(({ data }) => (data.data ?? []).map(parseConnection));
}

export function installDockerMcp(catalogServerId: string): Promise<McpConnection> {
  return apiClient.post<BaseResponse<unknown>>("/mcp/connections/docker", { catalogServerId })
    .then(({ data }) => parseConnection(first(data)));
}

export function createRemoteMcp(input: RemoteMcpConnectionInput): Promise<McpConnection> {
  return apiClient.post<BaseResponse<unknown>>("/mcp/connections/remote", input)
    .then(({ data }) => parseConnection(first(data)));
}

export function discoverMcpConnection(id: string): Promise<McpConnection> {
  return apiClient.post<BaseResponse<unknown>>(`/mcp/connections/${id}/discover`)
    .then(({ data }) => parseConnection(first(data)));
}

export function updateMcpTools({ id, enabledToolNames }: McpToolSelectionInput): Promise<McpConnection> {
  return apiClient.put<BaseResponse<unknown>>(`/mcp/connections/${id}/tools`, { enabledToolNames })
    .then(({ data }) => parseConnection(first(data)));
}

export function updateMcpConnectionState({ id, enabled }: McpConnectionStateInput): Promise<McpConnection> {
  return apiClient.put<BaseResponse<unknown>>(`/mcp/connections/${id}/state`, { enabled })
    .then(({ data }) => parseConnection(first(data)));
}

export function deleteMcpConnection(id: string): Promise<void> {
  return apiClient.delete(`/mcp/connections/${id}`).then(() => undefined);
}
