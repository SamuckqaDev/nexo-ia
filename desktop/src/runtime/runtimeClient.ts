import { randomUUID } from "node:crypto";
import WebSocket from "ws";
import {
  RUNTIME_PROTOCOL,
  type DeviceConfiguration,
  type LocalWorkspaceBinding,
  type RuntimeEnvelope
} from "../protocol/types.js";
import { RuntimeToolError, type WorkspaceTools } from "./workspaceTools.js";

const capabilities = [
  "workspace_list_files",
  "workspace_read_file",
  "workspace_read_file_raw",
  "workspace_write_file",
  "workspace_delete_file",
  "workspace_search",
  "workspace_inspect_project",
  "workspace_git_status",
  "workspace_git_diff"
];

export class RuntimeClient {
  private socket: WebSocket | null = null;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private workspaceRefreshTimer: NodeJS.Timeout | null = null;
  private sequence = 0;
  private stopped = false;

  constructor(
    private readonly configuration: () => DeviceConfiguration | null,
    private readonly workspaces: () => LocalWorkspaceBinding[],
    private readonly tools: WorkspaceTools,
    private readonly onStatus: (connected: boolean) => void
  ) {}

  connected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  start(): void {
    this.stopped = false;
    this.connect();
  }

  restart(): void {
    this.stop();
    this.stopped = false;
    this.connect();
  }

  stop(): void {
    this.stopped = true;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.heartbeatTimer) clearInterval(this.heartbeatTimer);
    if (this.workspaceRefreshTimer) clearInterval(this.workspaceRefreshTimer);
    this.reconnectTimer = null;
    this.heartbeatTimer = null;
    this.workspaceRefreshTimer = null;
    this.socket?.close();
    this.socket = null;
    this.onStatus(false);
  }

  private connect(): void {
    const device = this.configuration();
    if (this.stopped || !device || this.socket) return;
    const endpoint = new URL("/api/v1/device-runtime/connect", device.serverUrl);
    endpoint.protocol = endpoint.protocol === "https:" ? "wss:" : "ws:";
    const socket = new WebSocket(endpoint, { headers: { Authorization: `Device ${device.credential}` } });
    this.socket = socket;
    socket.on("open", (): void => {
      this.onStatus(true);
      this.sendEvent("runtime.capabilities", { capabilities });
      void this.synchronizeBindings(device);
      this.heartbeatTimer = setInterval(
        (): void => this.sendEvent("runtime.heartbeat", {}),
        20_000
      );
      this.workspaceRefreshTimer = setInterval(
        (): void => { void this.synchronizeBindings(device); },
        60_000
      );
    });
    socket.on("message", (message: WebSocket.RawData): void => {
      this.receive(message.toString()).catch((): void => socket.close(1011, "Runtime request failed"));
    });
    socket.on("close", (): void => this.disconnected(socket));
    socket.on("error", (): void => socket.close());
  }

  private disconnected(socket: WebSocket): void {
    if (this.socket !== socket) return;
    this.socket = null;
    if (this.heartbeatTimer) clearInterval(this.heartbeatTimer);
    if (this.workspaceRefreshTimer) clearInterval(this.workspaceRefreshTimer);
    this.heartbeatTimer = null;
    this.workspaceRefreshTimer = null;
    this.onStatus(false);
    if (!this.stopped) {
      this.reconnectTimer = setTimeout((): void => this.connect(), 2_500);
    }
  }

  private async receive(raw: string): Promise<void> {
    const envelope = JSON.parse(raw) as RuntimeEnvelope;
    if (envelope.protocol !== RUNTIME_PROTOCOL || envelope.type !== "request") return;
    try {
      const result = await this.tools.execute(envelope.method, envelope.payload);
      this.send({ ...envelope, type: "response", sequence: ++this.sequence,
        timestamp: new Date().toISOString(), payload: result, error: null });
    } catch (error: unknown) {
      const controlled = error instanceof RuntimeToolError
        ? error
        : new RuntimeToolError("FAILED", "The local runtime could not complete this request");
      this.send({ ...envelope, type: "response", sequence: ++this.sequence,
        timestamp: new Date().toISOString(), payload: {},
        error: { code: controlled.code, message: controlled.message } });
    }
  }

  private sendEvent(method: string, payload: unknown): void {
    this.send({
      protocol: RUNTIME_PROTOCOL,
      type: "event",
      id: randomUUID(),
      runId: null,
      taskId: null,
      sequence: ++this.sequence,
      timestamp: new Date().toISOString(),
      method,
      payload,
      error: null
    });
  }

  private async synchronizeBindings(device: DeviceConfiguration): Promise<void> {
    await Promise.allSettled(this.workspaces().map(async (workspace): Promise<void> => {
      const snapshot = await this.tools.inspectBinding(workspace.rootPath)
        .then((inspection) => ({ status: "AVAILABLE", ...inspection }))
        .catch(() => ({
          status: "MISSING",
          structureFingerprint: null,
          gitHead: null,
          gitBranch: null
        }));
      await fetch(
        new URL(`/api/v1/device-runtime/workspaces/${workspace.workspaceId}/bindings`, device.serverUrl),
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            Authorization: `Device ${device.credential}`
          },
          body: JSON.stringify({
            localBindingId: workspace.localBindingId,
            displayName: workspace.displayName,
            ...snapshot
          })
        }
      ).then((response): void => {
        if (!response.ok) throw new Error("Nexo Server rejected the local workspace refresh");
      });
    }));
  }

  private send(envelope: RuntimeEnvelope): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(envelope));
    }
  }
}
