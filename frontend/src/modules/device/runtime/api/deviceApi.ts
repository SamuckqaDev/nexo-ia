import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { devicePairingSchema, deviceSchema } from "../schemas/deviceSchemas";
import type { Device, DevicePairing } from "../types/deviceTypes";

const first = <T>(response: BaseResponse<unknown>, parse: (value: unknown) => T): T => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty device response");
  return parse(value);
};

export const listDevices = (): Promise<Device[]> =>
  apiClient.get<BaseResponse<unknown>>("/devices")
    .then(({ data }) => (data.data ?? []).map((value: unknown) => deviceSchema.parse(value)));

export const createDevicePairing = (): Promise<DevicePairing> =>
  apiClient.post<BaseResponse<unknown>>("/devices/pairings")
    .then(({ data }) => first(data, (value: unknown) => devicePairingSchema.parse(value)));

export const revokeDevice = (deviceId: string): Promise<void> =>
  apiClient.delete<BaseResponse<unknown>>(`/devices/${deviceId}`).then(() => undefined);
