import type { McpConnection } from "../types/mcpTypes";

export const machineProfileServerId = "docker-profile";

export function isMachineProfile(connection: McpConnection): boolean {
  return connection.catalogServerId === machineProfileServerId;
}

export function countAgentTools(connection: McpConnection): number {
  return isMachineProfile(connection)
    ? connection.tools.length
    : connection.tools.filter((tool) => tool.enabled).length;
}
