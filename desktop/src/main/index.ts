import { randomUUID } from "node:crypto";
import { app, BrowserWindow, dialog, ipcMain } from "electron";
import { basename, join } from "node:path";
import { fileURLToPath } from "node:url";
import type {
  ChooseWorkspaceInput,
  DesktopState,
  DeviceConfiguration,
  LocalWorkspaceBinding,
  PairDesktopInput
} from "../protocol/types.js";
import { RuntimeClient } from "../runtime/runtimeClient.js";
import { SecureRuntimeStore } from "../runtime/secureStore.js";
import { WorkspaceTools } from "../runtime/workspaceTools.js";

let mainWindow: BrowserWindow | null = null;
let connected = false;
let store: SecureRuntimeStore;
let tools: WorkspaceTools;
let runtime: RuntimeClient;

const safeState = (): DesktopState => ({
  paired: store.device() !== null,
  connected,
  deviceId: store.device()?.deviceId ?? null,
  serverUrl: store.device()?.serverUrl ?? null,
  workspaces: store.workspaces().map(({ rootPath: _rootPath, ...workspace }) => workspace)
});

const normalizeServerUrl = (value: string): string => {
  const url = new URL(value.trim());
  if (!['http:', 'https:'].includes(url.protocol)) throw new Error("Nexo Server must use HTTP or HTTPS");
  url.pathname = "/";
  url.search = "";
  url.hash = "";
  return url.toString();
};

const pair = async (input: PairDesktopInput): Promise<DesktopState> => {
  const serverUrl = normalizeServerUrl(input.serverUrl);
  const response = await fetch(new URL("/api/v1/device-runtime/pair", serverUrl), {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      pairingCode: input.pairingCode,
      displayName: input.displayName,
      platform: process.platform,
      architecture: process.arch,
      appVersion: app.getVersion()
    })
  });
  const body = await response.json() as { message?: string; data?: Array<{ deviceId: string; credential: string }> };
  const paired = body.data?.[0];
  if (!response.ok || !paired) throw new Error(body.message ?? "Nexo Desktop could not pair with the server");
  await store.saveDevice({ serverUrl, deviceId: paired.deviceId, credential: paired.credential });
  runtime.restart();
  return safeState();
};

const chooseWorkspace = async (input: ChooseWorkspaceInput): Promise<DesktopState> => {
  const device = store.device();
  if (!device) throw new Error("Pair Nexo Desktop before selecting a local workspace");
  const dialogOptions = {
    title: `Choose the local folder for ${input.workspaceName}`,
    properties: ["openDirectory", "createDirectory"] as Array<"openDirectory" | "createDirectory">
  };
  const selection = mainWindow
    ? await dialog.showOpenDialog(mainWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions);
  const rootPath = selection.filePaths[0];
  if (selection.canceled || !rootPath) return safeState();
  const inspection = await tools.inspectBinding(rootPath);
  const binding: LocalWorkspaceBinding = {
    localBindingId: randomUUID(),
    workspaceId: input.workspaceId,
    displayName: basename(rootPath),
    rootPath,
    ...inspection
  };
  const response = await fetch(
    new URL(`/api/v1/device-runtime/workspaces/${input.workspaceId}/bindings`, device.serverUrl),
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        Authorization: `Device ${device.credential}`
      },
      body: JSON.stringify({
        localBindingId: binding.localBindingId,
        displayName: binding.displayName,
        status: "AVAILABLE",
        structureFingerprint: binding.structureFingerprint,
        gitHead: binding.gitHead,
        gitBranch: binding.gitBranch
      })
    }
  );
  const body = await response.json() as { message?: string };
  if (!response.ok) throw new Error(body.message ?? "Nexo could not register the local workspace binding");
  await store.saveWorkspace(binding);
  return safeState();
};

const createWindow = (): void => {
  const preload = fileURLToPath(new URL("../preload/index.js", import.meta.url));
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 940,
    minWidth: 900,
    minHeight: 640,
    backgroundColor: "#071226",
    webPreferences: { preload, contextIsolation: true, nodeIntegration: false, sandbox: true }
  });
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: "deny" }));
  mainWindow.webContents.on("will-navigate", (event, url): void => {
    const rendererUrl = process.env.NEXO_RENDERER_URL;
    if (rendererUrl && url.startsWith(rendererUrl)) return;
    if (!rendererUrl && url.startsWith("file:")) return;
    event.preventDefault();
  });
  const rendererUrl = process.env.NEXO_RENDERER_URL;
  if (rendererUrl) {
    void mainWindow.loadURL(rendererUrl);
  } else {
    const packaged = join(process.resourcesPath, "frontend", "index.html");
    const development = fileURLToPath(new URL("../../../frontend/dist/index.html", import.meta.url));
    void mainWindow.loadFile(app.isPackaged ? packaged : development);
  }
};

app.whenReady().then(async (): Promise<void> => {
  store = new SecureRuntimeStore();
  await store.load();
  tools = new WorkspaceTools(store);
  runtime = new RuntimeClient(
    (): DeviceConfiguration | null => store.device(),
    (): LocalWorkspaceBinding[] => store.workspaces(),
    tools,
    (value: boolean): void => {
      connected = value;
      mainWindow?.webContents.send("nexo-desktop:status", safeState());
    }
  );
  ipcMain.handle("nexo-desktop:state", (): DesktopState => safeState());
  ipcMain.handle("nexo-desktop:pair", (_event, input: PairDesktopInput): Promise<DesktopState> => pair(input));
  ipcMain.handle(
    "nexo-desktop:choose-workspace",
    (_event, input: ChooseWorkspaceInput): Promise<DesktopState> => chooseWorkspace(input)
  );
  runtime.start();
  createWindow();

  app.on("activate", (): void => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
}).catch((): void => app.quit());

app.on("window-all-closed", (): void => {
  runtime?.stop();
  if (process.platform !== "darwin") app.quit();
});
