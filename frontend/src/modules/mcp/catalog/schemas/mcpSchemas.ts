import { z } from "zod";

export const mcpCostTypeSchema = z.enum([
  "LOCAL_FREE",
  "FREE_TIER",
  "ACCOUNT_REQUIRED",
  "PAID",
  "UNKNOWN"
]);

export const mcpCatalogServerSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string(),
  category: z.string(),
  image: z.string().nullable(),
  iconUrl: z.string().nullable(),
  license: z.string().nullable(),
  costType: mcpCostTypeSchema,
  riskLevel: z.enum(["READ_ONLY", "READ_WRITE", "UNKNOWN"]),
  requiresSecrets: z.boolean(),
  requiresConfiguration: z.boolean(),
  toolCount: z.number().int().nonnegative(),
  recommended: z.boolean()
});

export const mcpCatalogSchema = z.object({
  dockerAvailable: z.boolean(),
  gatewayVersion: z.string().nullable(),
  source: z.string(),
  refreshedAt: z.iso.datetime(),
  servers: z.array(mcpCatalogServerSchema)
});

export const mcpToolSchema = z.object({
  externalName: z.string(),
  exposedName: z.string(),
  title: z.string().nullable(),
  description: z.string().nullable(),
  enabled: z.boolean(),
  readOnlyHint: z.boolean().nullable(),
  destructiveHint: z.boolean().nullable(),
  openWorldHint: z.boolean().nullable(),
  discoveredAt: z.iso.datetime()
});

export const mcpConnectionSchema = z.object({
  id: z.uuid(),
  displayName: z.string(),
  connectionKind: z.enum(["DOCKER_CATALOG", "CUSTOM_REMOTE"]),
  transportType: z.enum(["DOCKER_GATEWAY", "STREAMABLE_HTTP"]),
  catalogServerId: z.string().nullable(),
  endpoint: z.string().nullable(),
  costType: mcpCostTypeSchema,
  status: z.enum(["PENDING", "CONNECTED", "UNAVAILABLE", "DISABLED"]),
  enabled: z.boolean(),
  serverName: z.string().nullable(),
  serverVersion: z.string().nullable(),
  lastErrorCode: z.string().nullable(),
  lastConnectedAt: z.iso.datetime().nullable(),
  tools: z.array(mcpToolSchema),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});

export const remoteMcpConnectionSchema = z.object({
  displayName: z.string().trim().min(2, "Enter a server name").max(100),
  endpoint: z.url("Enter a valid HTTP or HTTPS MCP URL").max(500)
});

export type RemoteMcpConnectionValues = z.infer<typeof remoteMcpConnectionSchema>;
