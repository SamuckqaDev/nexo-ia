import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import {
  createRemoteMcp,
  deleteMcpConnection,
  discoverMcpConnection,
  getMcpCatalog,
  installDockerMcp,
  listMcpConnections,
  updateMcpConnectionState,
  updateMcpTools
} from "../api/mcpApi";
import type { McpConnection, McpHubResult, RemoteMcpConnectionInput } from "../types/mcpTypes";

export function useMcpHub(): McpHubResult {
  const queryClient: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const refresh = (): void => { queryClient.invalidateQueries({ queryKey: ["mcp", "connections"] }); };
  const failure = (error: Error): void => show(error.message, { variant: "error" });

  const catalog = useQuery({
    queryKey: ["mcp", "catalog"],
    queryFn: getMcpCatalog,
    retry: false,
    staleTime: 5 * 60 * 1000
  });
  const connections = useQuery({
    queryKey: ["mcp", "connections"],
    queryFn: listMcpConnections,
    retry: false
  });
  const installDocker = useMutation({
    mutationFn: (catalogServerId: string): Promise<McpConnection> => installDockerMcp(catalogServerId)
      .then((connection: McpConnection) => discoverMcpConnection(connection.id)),
    onSuccess: (): void => { refresh(); show("Docker MCP inspected. Select its tools to continue.", { variant: "success" }); },
    onError: (error: Error): void => { refresh(); failure(error); }
  });
  const createRemote = useMutation({
    mutationFn: (input: RemoteMcpConnectionInput): Promise<McpConnection> => createRemoteMcp(input)
      .then((connection: McpConnection) => discoverMcpConnection(connection.id)),
    onSuccess: (): void => { refresh(); show("Remote MCP inspected. Select its tools to continue.", { variant: "success" }); },
    onError: (error: Error): void => { refresh(); failure(error); }
  });
  const discover = useMutation({
    mutationFn: discoverMcpConnection,
    onSuccess: (): void => { refresh(); show("MCP tool snapshot refreshed.", { variant: "success" }); },
    onError: (error: Error): void => { refresh(); failure(error); }
  });
  const selectTools = useMutation({
    mutationFn: updateMcpTools,
    onSuccess: (): void => { refresh(); show("Allowed MCP tools updated.", { variant: "success" }); },
    onError: failure
  });
  const setEnabled = useMutation({
    mutationFn: updateMcpConnectionState,
    onSuccess: (connection: McpConnection): void => {
      refresh();
      show(connection.enabled ? "MCP enabled for Agent mode." : "MCP disabled.", { variant: "success" });
    },
    onError: failure
  });
  const remove = useMutation({
    mutationFn: deleteMcpConnection,
    onSuccess: (): void => { refresh(); show("MCP connection removed.", { variant: "success" }); },
    onError: failure
  });

  return { catalog, connections, installDocker, createRemote, discover, selectTools, setEnabled, remove };
}
