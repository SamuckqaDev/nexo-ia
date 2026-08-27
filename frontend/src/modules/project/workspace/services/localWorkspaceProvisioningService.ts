import type { DesktopRuntimeHook } from "../../../device/runtime/hooks/useDesktopRuntime";
import { createServerWorkspace, deleteServerWorkspace } from "../api/serverWorkspaceApi";
import type { ServerWorkspace } from "../types/serverWorkspaceTypes";

type ProvisioningRuntime = Pick<
  DesktopRuntimeHook,
  "selectWorkspaceDirectory" | "chooseWorkspace"
>;

type ProvisioningDependencies = {
  createWorkspace: typeof createServerWorkspace;
  deleteWorkspace: typeof deleteServerWorkspace;
};

const defaultDependencies: ProvisioningDependencies = {
  createWorkspace: createServerWorkspace,
  deleteWorkspace: deleteServerWorkspace
};

/** Creates the server registration only after the native folder chooser has been confirmed. */
export async function provisionLocalWorkspace(
  runtime: ProvisioningRuntime,
  dependencies: ProvisioningDependencies = defaultDependencies
): Promise<ServerWorkspace | null> {
  const selection = await runtime.selectWorkspaceDirectory();
  if (!selection) return null;

  const workspace = await dependencies.createWorkspace({
    name: selection.displayName,
    storageType: "UNBOUND",
    accessMode: "READ_ONLY"
  });

  try {
    await runtime.chooseWorkspace(workspace.id, workspace.name, selection.selectionId);
    return workspace;
  } catch (error: unknown) {
    await dependencies.deleteWorkspace(workspace.id).catch(() => undefined);
    throw error;
  }
}
