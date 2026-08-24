import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import type { z } from "zod";
import type {
  mcpCatalogSchema,
  mcpCatalogServerSchema,
  mcpConnectionSchema,
  mcpToolSchema,
  remoteMcpConnectionSchema
} from "../schemas/mcpSchemas";

export type McpCatalog = z.infer<typeof mcpCatalogSchema>;
export type McpCatalogServer = z.infer<typeof mcpCatalogServerSchema>;
export type McpConnection = z.infer<typeof mcpConnectionSchema>;
export type McpTool = z.infer<typeof mcpToolSchema>;
export type RemoteMcpConnectionInput = z.infer<typeof remoteMcpConnectionSchema>;

export type McpConnectionStateInput = { id: string; enabled: boolean };
export type McpToolSelectionInput = { id: string; enabledToolNames: string[] };

export type McpHubResult = {
  catalog: UseQueryResult<McpCatalog, Error>;
  connections: UseQueryResult<McpConnection[], Error>;
  installDocker: UseMutationResult<McpConnection, Error, string>;
  createRemote: UseMutationResult<McpConnection, Error, RemoteMcpConnectionInput>;
  discover: UseMutationResult<McpConnection, Error, string>;
  selectTools: UseMutationResult<McpConnection, Error, McpToolSelectionInput>;
  setEnabled: UseMutationResult<McpConnection, Error, McpConnectionStateInput>;
  remove: UseMutationResult<void, Error, string>;
};
