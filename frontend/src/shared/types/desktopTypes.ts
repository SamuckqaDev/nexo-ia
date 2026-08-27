export type DesktopWorkspace = {
  localBindingId: string;
  workspaceId: string;
  displayName: string;
  structureFingerprint: string;
  gitHead: string | null;
  gitBranch: string | null;
};

export type DesktopRuntimeState = {
  paired: boolean;
  connected: boolean;
  deviceId: string | null;
  serverUrl: string | null;
  workspaces: DesktopWorkspace[];
};

export type DesktopPairingInput = {
  serverUrl: string;
  pairingCode: string;
  displayName: string;
};

export type DesktopBridge = {
  state: () => Promise<DesktopRuntimeState>;
  pair: (input: DesktopPairingInput) => Promise<DesktopRuntimeState>;
  chooseWorkspace: (input: { workspaceId: string; workspaceName: string }) => Promise<DesktopRuntimeState>;
  onStatus: (listener: (state: DesktopRuntimeState) => void) => () => void;
};

declare global {
  interface Window {
    nexoDesktop?: DesktopBridge;
  }
}
