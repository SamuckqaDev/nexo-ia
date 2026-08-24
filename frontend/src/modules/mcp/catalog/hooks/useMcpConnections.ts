import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { listMcpConnections } from "../api/mcpApi";
import type { McpConnection } from "../types/mcpTypes";

export const mcpConnectionsKey = ["mcp", "connections"] as const;

export function useMcpConnections(enabled = true): UseQueryResult<McpConnection[], Error> {
  return useQuery({
    queryKey: mcpConnectionsKey,
    queryFn: listMcpConnections,
    enabled,
    retry: false
  });
}
