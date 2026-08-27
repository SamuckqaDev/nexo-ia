export const RUNTIME_PROTOCOL = "nexo.runtime.v1" as const;

export type RuntimeError = {
  code: string;
  message: string;
};

export type RuntimeEnvelope = {
  protocol: typeof RUNTIME_PROTOCOL;
  type: "request" | "response" | "event";
  id: string;
  runId: string | null;
  taskId: string | null;
  sequence: number;
  timestamp: string;
  method: string;
  payload: unknown;
  error: RuntimeError | null;
};

export type DeviceConfiguration = {
  serverUrl: string;
  deviceId: string;
  credential: string;
};

export type LocalWorkspaceBinding = {
  localBindingId: string;
  workspaceId: string;
  displayName: string;
  rootPath: string;
  structureFingerprint: string;
  gitHead: string | null;
  gitBranch: string | null;
};

export type DesktopWorkspaceSelection = {
  selectionId: string;
  displayName: string;
};

export type DesktopState = {
  paired: boolean;
  connected: boolean;
  deviceId: string | null;
  serverUrl: string | null;
  workspaces: Array<Omit<LocalWorkspaceBinding, "rootPath">>;
};

export type PairDesktopInput = {
  serverUrl: string;
  pairingCode: string;
  displayName: string;
};

export type ChooseWorkspaceInput = {
  workspaceId: string;
  workspaceName: string;
  selectionId?: string;
};
