import {
  ArrowsClockwise,
  CheckCircle,
  Cloud,
  Cube,
  MagnifyingGlass,
  PlugsConnected,
  Plus,
  ShieldCheck,
  Trash,
  WarningCircle,
  Wrench
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Loading } from "../../../../../shared/components/Loading";
import {
  WorkspaceBadge,
  WorkspaceEmptyState,
  WorkspacePage,
  WorkspacePanel,
  WorkspaceSegmentedControl
} from "../../../../../shared/components/WorkspacePage";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import { McpConnectionForm } from "../../components/McpConnectionForm";
import { useMcpHub } from "../../hooks/useMcpHub";
import type {
  McpCatalogServer,
  McpConnection,
  McpTool,
  RemoteMcpConnectionInput
} from "../../types/mcpTypes";
import {
  CatalogCard,
  CatalogList,
  CatalogMeta,
  ConnectionButton,
  ConnectionCopy,
  ConnectionList,
  DetailActions,
  DetailBody,
  DetailHeader,
  EmptyTools,
  HubGrid,
  HubLayout,
  InlineNotice,
  PanelContent,
  ServerIdentity,
  ToolButton,
  ToolCopy,
  ToolList,
  Worlds
} from "./styles";

type CatalogFilter = "free" | "all";

const costLabel: Record<McpCatalogServer["costType"], string> = {
  LOCAL_FREE: "Local & free",
  FREE_TIER: "Free tier",
  ACCOUNT_REQUIRED: "Account",
  PAID: "Paid",
  UNKNOWN: "Unknown cost"
};

const statusLabel: Record<McpConnection["status"], string> = {
  PENDING: "Needs inspection",
  CONNECTED: "Ready",
  UNAVAILABLE: "Unavailable",
  DISABLED: "Off"
};

const failureMessage = (error: unknown): string => error instanceof Error
  ? error.message
  : "Nexo could not load this MCP surface.";

export function McpHubPage(): ReactElement {
  const hub = useMcpHub();
  const catalog = hub.catalog.data;
  const connections: McpConnection[] = hub.connections.data ?? [];
  const [query, setQuery] = useState<string>("");
  const [filter, setFilter] = useState<CatalogFilter>("free");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [creatingRemote, setCreatingRemote] = useState<boolean>(false);
  const [selectedTools, setSelectedTools] = useState<string[]>([]);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);

  const selected: McpConnection | undefined = connections.find(
    (connection: McpConnection) => connection.id === selectedId);
  const installedIds = useMemo<Set<string>>(
    () => new Set(connections.map((connection: McpConnection) => connection.catalogServerId).filter(Boolean) as string[]),
    [connections]
  );
  const visibleServers = useMemo<McpCatalogServer[]>(() => {
    const search = query.trim().toLowerCase();
    return (catalog?.servers ?? []).filter((server: McpCatalogServer) => {
      const matchesSearch = !search || `${server.title} ${server.description} ${server.category}`
        .toLowerCase().includes(search);
      const matchesCost = filter === "all"
        || server.costType === "LOCAL_FREE"
        || server.costType === "FREE_TIER";
      return matchesSearch && matchesCost;
    });
  }, [catalog?.servers, filter, query]);
  const toolSelectionDirty: boolean = selected
    ? selected.tools.filter((tool: McpTool) => tool.enabled)
      .map((tool: McpTool) => tool.externalName).sort().join("\n")
      !== [...selectedTools].sort().join("\n")
    : false;

  useEffect((): void => {
    if (!selectedId && connections.length) setSelectedId(connections[0].id);
    if (selectedId && !connections.some((connection: McpConnection) => connection.id === selectedId)) {
      setSelectedId(connections[0]?.id ?? null);
    }
  }, [connections, selectedId]);

  useEffect((): void => {
    setSelectedTools(selected?.tools.filter((tool: McpTool) => tool.enabled)
      .map((tool: McpTool) => tool.externalName) ?? []);
  }, [selected]);

  const install = (server: McpCatalogServer): void => {
    hub.installDocker.mutate(server.id, {
      onSuccess: (connection: McpConnection): void => setSelectedId(connection.id)
    });
  };
  const createRemote = (input: RemoteMcpConnectionInput): void => {
    hub.createRemote.mutate(input, {
      onSuccess: (connection: McpConnection): void => {
        setSelectedId(connection.id);
        setCreatingRemote(false);
      }
    });
  };
  const toggleTool = (name: string): void => {
    setSelectedTools((current: string[]) => current.includes(name)
      ? current.filter((value: string) => value !== name)
      : [...current, name]);
  };
  const enableForAgent = (connection: McpConnection): void => {
    if (toolSelectionDirty) {
      hub.selectTools.mutate(
        { id: connection.id, enabledToolNames: selectedTools },
        { onSuccess: (): void => hub.setEnabled.mutate({ id: connection.id, enabled: true }) }
      );
      return;
    }
    hub.setEnabled.mutate({ id: connection.id, enabled: true });
  };
  const remove = (connection: McpConnection): void => {
    ask({
      title: "Remove this MCP server?",
      message: `“${connection.displayName}” and its discovered tool snapshot will be removed.`,
      confirmLabel: "Remove server",
      tone: "danger"
    }).then((confirmed: boolean): void => {
      if (confirmed) hub.remove.mutate(connection.id);
    });
  };

  return (
    <WorkspacePage
      eyebrow="Agent integrations"
      title="MCP Hub"
      description="Connect Docker's reviewed catalog or your own Streamable HTTP server. Every connection belongs to you, and only tools you explicitly allow reach Agent mode."
      icon={PlugsConnected}
      contentMode="contained"
      actions={catalog ? (
        <WorkspaceBadge tone={catalog.dockerAvailable ? "positive" : "attention"}>
          {catalog.dockerAvailable ? `Docker MCP ${catalog.gatewayVersion ?? "ready"}` : "Docker MCP unavailable"}
        </WorkspaceBadge>
      ) : undefined}
    >
      <HubLayout>
        <Worlds>
          <span><Cube size={17} weight="duotone" /><strong>Docker world</strong> reviewed, containerized servers</span>
          <span><Cloud size={17} weight="duotone" /><strong>Your world</strong> personal remote MCP endpoints</span>
          <span><ShieldCheck size={17} weight="duotone" /><strong>One gate</strong> explicit tools, Agent mode only</span>
        </Worlds>

        <HubGrid>
        <WorkspacePanel title="Docker catalog" description="Free-first MCP servers from Docker's live catalog.">
          <PanelContent>
            <Input
              id="mcp-search"
              label="Find MCP servers"
              icon={MagnifyingGlass}
              value={query}
              onChange={(event): void => setQuery(event.target.value)}
              placeholder="Search tools or categories"
            />
            <WorkspaceSegmentedControl
              label="Catalog cost filter"
              value={filter}
              options={[{ value: "free", label: "Free first" }, { value: "all", label: "All" }]}
              onChange={setFilter}
            />
            {hub.catalog.isError ? (
              <InlineNotice><WarningCircle size={17} />{failureMessage(hub.catalog.error)}</InlineNotice>
            ) : hub.catalog.isLoading ? <Loading label="Reading Docker MCP Catalog…" /> : (
              <CatalogList>
                {catalog && !catalog.dockerAvailable && (
                  <InlineNotice>
                    <WarningCircle size={17} />
                    Docker MCP is not connected to this Nexo runtime. Catalog cards are informational
                    here; use Connect custom for a Streamable HTTP server or run the backend where
                    Docker MCP is available.
                  </InlineNotice>
                )}
                {visibleServers.map((server: McpCatalogServer) => {
                  const installed = installedIds.has(server.id);
                  const unsupportedSetup = server.requiresSecrets || server.requiresConfiguration;
                  const unavailable = !catalog?.dockerAvailable;
                  const installing = hub.installDocker.isPending
                    && hub.installDocker.variables === server.id;
                  return (
                    <CatalogCard key={server.id}>
                      <ServerIdentity>
                        <Cube size={20} weight="duotone" />
                        <div><strong>{server.title}</strong><span>{server.category}</span></div>
                      </ServerIdentity>
                      <p>{server.description}</p>
                      <CatalogMeta>
                        <WorkspaceBadge tone={server.costType === "LOCAL_FREE" ? "positive" : "default"}>
                          {costLabel[server.costType]}
                        </WorkspaceBadge>
                        <span>{server.toolCount || "?"} tools</span>
                        <span>{server.riskLevel === "READ_ONLY" ? "Read only" : "May write"}</span>
                      </CatalogMeta>
                      <Button
                        size="compact"
                        type="button"
                        variant="outline"
                        icon={installed ? CheckCircle : unavailable ? WarningCircle : Plus}
                        disabled={installed || unsupportedSetup || unavailable || hub.installDocker.isPending}
                        aria-busy={installing}
                        title={unsupportedSetup
                          ? "Per-user credentials and configuration are not stored yet"
                          : unavailable ? "The current Nexo backend cannot execute Docker MCP" : undefined}
                        onClick={(): void => install(server)}
                      >
                        {installed
                          ? "Installed"
                          : unsupportedSetup
                            ? "Setup coming next"
                            : unavailable
                              ? "Docker runtime unavailable"
                              : installing ? "Installing & inspecting…" : "Install & inspect"}
                      </Button>
                    </CatalogCard>
                  );
                })}
              </CatalogList>
            )}
          </PanelContent>
        </WorkspacePanel>

        <WorkspacePanel
          title="Your MCP servers"
          description="Private registrations; other users cannot list or execute them."
          action={<Button size="compact" type="button" variant="outline" icon={Plus} onClick={(): void => setCreatingRemote(true)}>Connect custom</Button>}
        >
          {creatingRemote ? (
            <McpConnectionForm
              pending={hub.createRemote.isPending}
              onSubmit={createRemote}
              onCancel={(): void => setCreatingRemote(false)}
            />
          ) : hub.connections.isError ? (
            <InlineNotice><WarningCircle size={17} />{failureMessage(hub.connections.error)}</InlineNotice>
          ) : hub.connections.isLoading ? <Loading label="Loading your MCP servers…" /> : connections.length ? (
            <ConnectionList>
              {connections.map((connection: McpConnection) => (
                <ConnectionButton
                  key={connection.id}
                  type="button"
                  $active={connection.id === selectedId}
                  onClick={(): void => setSelectedId(connection.id)}
                >
                  {connection.connectionKind === "DOCKER_CATALOG"
                    ? <Cube size={19} weight="duotone" />
                    : <Cloud size={19} weight="duotone" />}
                  <ConnectionCopy>
                    <strong>{connection.displayName}</strong>
                    <span>
                      {statusLabel[connection.status]}
                      {connection.status === "CONNECTED" && !connection.enabled ? " · Off in Agent" : ""}
                      {` · ${connection.tools.length} tools`}
                    </span>
                  </ConnectionCopy>
                  {connection.enabled
                    ? <CheckCircle size={16} weight="fill" />
                    : connection.status === "UNAVAILABLE" ? <WarningCircle size={16} /> : null}
                </ConnectionButton>
              ))}
            </ConnectionList>
          ) : (
            <WorkspaceEmptyState
              icon={PlugsConnected}
              title="No MCP servers yet"
              description={catalog?.dockerAvailable
                ? "Install a zero-secret Docker server or connect an MCP endpoint you control."
                : "Connect a public HTTPS Streamable HTTP endpoint. Docker cards require a Nexo runtime with Docker MCP available."}
            />
          )}
        </WorkspacePanel>

        <WorkspacePanel
          title={selected?.displayName ?? "MCP inspector"}
          description={selected ? "Discover capabilities, choose tools, then enable them for Agent mode." : "Select one of your servers."}
        >
          {selected ? (
            <DetailBody>
              <DetailHeader>
                <div>
                  <WorkspaceBadge tone={selected.enabled ? "positive" : selected.status === "UNAVAILABLE" ? "attention" : "default"}>
                    {selected.enabled
                      ? "Active in Agent"
                      : selected.status === "CONNECTED" ? "Ready · Off in Agent" : statusLabel[selected.status]}
                  </WorkspaceBadge>
                  <span>{selected.serverName ?? selected.catalogServerId ?? selected.endpoint}</span>
                  {selected.serverVersion && <small>Version {selected.serverVersion}</small>}
                </div>
                <DetailActions>
                  <Button
                    size="compact"
                    type="button"
                    variant="outline"
                    icon={ArrowsClockwise}
                    disabled={hub.discover.isPending}
                    aria-busy={hub.discover.isPending}
                    onClick={(): void => hub.discover.mutate(selected.id)}
                  >
                    {hub.discover.isPending ? "Inspecting…" : "Inspect"}
                  </Button>
                  <Button size="compact" type="button" variant="outline" icon={Trash} onClick={(): void => remove(selected)}>Remove</Button>
                </DetailActions>
              </DetailHeader>

              {selected.lastErrorCode && (
                <InlineNotice><WarningCircle size={17} />The server could not be reached. Check Docker or the endpoint and inspect again.</InlineNotice>
              )}

              {!selected.enabled && selected.status === "CONNECTED" && selectedTools.length > 0 && (
                <InlineNotice>
                  <WarningCircle size={17} />
                  {selectedTools.length} allowed tool{selectedTools.length === 1 ? " is" : "s are"} selected,
                  but {selected.displayName} is still off in Agent. Enable it below before returning to Chat.
                </InlineNotice>
              )}

              {selected.tools.length ? (
                <>
                  <ToolList>
                    {selected.tools.map((tool: McpTool) => {
                      const checked = selectedTools.includes(tool.externalName);
                      return (
                        <ToolButton key={tool.externalName} type="button" $active={checked} onClick={(): void => toggleTool(tool.externalName)}>
                          <span aria-hidden>{checked ? <CheckCircle size={18} weight="fill" /> : <Wrench size={18} />}</span>
                          <ToolCopy>
                            <strong>{tool.title ?? tool.externalName}</strong>
                            <code>{tool.externalName}</code>
                            <p>{tool.description ?? "No tool description was provided by this server."}</p>
                          </ToolCopy>
                          <WorkspaceBadge tone={tool.destructiveHint ? "attention" : tool.readOnlyHint ? "positive" : "default"}>
                            {tool.destructiveHint ? "Destructive" : tool.readOnlyHint ? "Read only" : "Review"}
                          </WorkspaceBadge>
                        </ToolButton>
                      );
                    })}
                  </ToolList>
                  <DetailActions>
                    <Button
                      size="compact"
                      type="button"
                      variant="outline"
                      disabled={!toolSelectionDirty || hub.selectTools.isPending}
                      aria-busy={hub.selectTools.isPending}
                      onClick={(): void => hub.selectTools.mutate({ id: selected.id, enabledToolNames: selectedTools })}
                    >
                      {hub.selectTools.isPending ? "Saving tools…" : "Save allowed tools"}
                    </Button>
                    <Button
                      size="compact"
                      type="button"
                      disabled={(!selected.enabled && selectedTools.length === 0)
                        || hub.setEnabled.isPending || hub.selectTools.isPending}
                      aria-busy={hub.setEnabled.isPending || hub.selectTools.isPending}
                      onClick={(): void => selected.enabled
                        ? hub.setEnabled.mutate({ id: selected.id, enabled: false })
                        : enableForAgent(selected)}
                    >
                      {hub.setEnabled.isPending || (hub.selectTools.isPending && !selected.enabled)
                        ? selected.enabled ? "Disabling…" : "Enabling…"
                        : selected.enabled
                          ? "Disable in Agent"
                          : toolSelectionDirty
                            ? `Save & enable ${selectedTools.length} tool${selectedTools.length === 1 ? "" : "s"}`
                            : `Enable ${selectedTools.length} tool${selectedTools.length === 1 ? "" : "s"} in Agent`}
                    </Button>
                  </DetailActions>
                </>
              ) : (
                <EmptyTools><Wrench size={24} /><strong>No tools discovered</strong><span>Inspect the connection to load its real MCP tool definitions.</span></EmptyTools>
              )}
            </DetailBody>
          ) : <WorkspaceEmptyState icon={Wrench} title="Choose a server" description="Its discovered tools and Agent access controls will appear here." />}
        </WorkspacePanel>
        </HubGrid>
      </HubLayout>
    </WorkspacePage>
  );
}
