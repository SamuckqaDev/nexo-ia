import { contextBridge, ipcRenderer } from "electron";
import type {
  ChooseWorkspaceInput,
  DesktopState,
  PairDesktopInput
} from "../protocol/types.js";

const desktopApi = {
  state: (): Promise<DesktopState> => ipcRenderer.invoke("nexo-desktop:state") as Promise<DesktopState>,
  pair: (input: PairDesktopInput): Promise<DesktopState> =>
    ipcRenderer.invoke("nexo-desktop:pair", input) as Promise<DesktopState>,
  chooseWorkspace: (input: ChooseWorkspaceInput): Promise<DesktopState> =>
    ipcRenderer.invoke("nexo-desktop:choose-workspace", input) as Promise<DesktopState>,
  onStatus: (listener: (state: DesktopState) => void): (() => void) => {
    const handler = (_event: Electron.IpcRendererEvent, state: DesktopState): void => listener(state);
    ipcRenderer.on("nexo-desktop:status", handler);
    return (): void => { ipcRenderer.removeListener("nexo-desktop:status", handler); };
  }
};

contextBridge.exposeInMainWorld("nexoDesktop", desktopApi);
