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

export type DesktopWorkspaceSelection = {
  selectionId: string;
  displayName: string;
  existingWorkspaceId: string | null;
};

export type DesktopPairingInput = {
  serverUrl: string;
  pairingCode: string;
  displayName: string;
};

export type DesktopBridge = {
  state: () => Promise<DesktopRuntimeState>;
  pair: (input: DesktopPairingInput) => Promise<DesktopRuntimeState>;
  selectWorkspaceDirectory: () => Promise<DesktopWorkspaceSelection | null>;
  chooseWorkspace: (input: {
    workspaceId: string;
    workspaceName: string;
    selectionId?: string;
  }) => Promise<DesktopRuntimeState>;
  onStatus: (listener: (state: DesktopRuntimeState) => void) => () => void;
};

declare global {
  interface Window {
    nexoDesktop?: DesktopBridge;
  }
}
