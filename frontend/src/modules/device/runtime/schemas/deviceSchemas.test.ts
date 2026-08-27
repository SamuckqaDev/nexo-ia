import { describe, expect, it } from "vitest";
import { devicePairingSchema, deviceSchema } from "./deviceSchemas";

describe("device runtime schemas", () => {
  it("accepts the paired-device response contract", () => {
    const parsed = deviceSchema.parse({
      id: "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23",
      displayName: "Samuel's Mac",
      platform: "macos",
      architecture: "arm64",
      appVersion: "0.1.0",
      status: "ONLINE",
      capabilities: ["workspace_read_file"],
      lastSeenAt: "2026-08-26T20:00:00Z",
      createdAt: "2026-08-26T19:59:00Z"
    });

    expect(parsed.status).toBe("ONLINE");
    expect(parsed.capabilities).toContain("workspace_read_file");
  });

  it("requires a real pairing expiry timestamp", () => {
    expect(() => devicePairingSchema.parse({ pairingCode: "one-time", expiresAt: null })).toThrow();
  });
});
