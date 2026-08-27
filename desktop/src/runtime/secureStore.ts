import { app, safeStorage } from "electron";
import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import type { DeviceConfiguration, LocalWorkspaceBinding } from "../protocol/types.js";

type RuntimeState = {
  device: DeviceConfiguration | null;
  workspaces: LocalWorkspaceBinding[];
};

const EMPTY_STATE: RuntimeState = { device: null, workspaces: [] };

export class SecureRuntimeStore {
  private readonly path: string;
  private state: RuntimeState = EMPTY_STATE;

  constructor() {
    this.path = join(app.getPath("userData"), "runtime-state.enc");
  }

  async load(): Promise<void> {
    if (!safeStorage.isEncryptionAvailable()) {
      throw new Error("The operating system credential encryption service is unavailable");
    }
    this.state = await readFile(this.path)
      .then((encrypted: Buffer): RuntimeState => JSON.parse(
        safeStorage.decryptString(encrypted)
      ) as RuntimeState)
      .catch((error: NodeJS.ErrnoException): RuntimeState => {
        if (error.code === "ENOENT") return EMPTY_STATE;
        throw error;
      });
  }

  device(): DeviceConfiguration | null {
    return this.state.device;
  }

  workspaces(): LocalWorkspaceBinding[] {
    return [...this.state.workspaces];
  }

  workspace(localBindingId: string): LocalWorkspaceBinding | null {
    return this.state.workspaces.find((item: LocalWorkspaceBinding): boolean =>
      item.localBindingId === localBindingId) ?? null;
  }

  async saveDevice(device: DeviceConfiguration): Promise<void> {
    this.state = { ...this.state, device };
    await this.persist();
  }

  async saveWorkspace(workspace: LocalWorkspaceBinding): Promise<void> {
    this.state = {
      ...this.state,
      workspaces: [
        workspace,
        ...this.state.workspaces.filter((item: LocalWorkspaceBinding): boolean =>
          item.localBindingId !== workspace.localBindingId)
      ]
    };
    await this.persist();
  }

  private async persist(): Promise<void> {
    const temporaryPath = `${this.path}.tmp`;
    await mkdir(dirname(this.path), { recursive: true });
    await writeFile(temporaryPath, safeStorage.encryptString(JSON.stringify(this.state)), { mode: 0o600 });
    await rename(temporaryPath, this.path);
  }
}
