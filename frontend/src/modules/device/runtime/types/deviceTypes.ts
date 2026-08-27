import type { z } from "zod";
import type { devicePairingSchema, deviceSchema } from "../schemas/deviceSchemas";

export type Device = z.infer<typeof deviceSchema>;
export type DevicePairing = z.infer<typeof devicePairingSchema>;

export type DesktopRuntimeCardProps = {
  workspaceId: string | null;
  workspaceName: string | null;
  onWorkspaceBound: () => void;
};
