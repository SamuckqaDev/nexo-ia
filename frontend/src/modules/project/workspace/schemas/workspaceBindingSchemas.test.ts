import { describe, expect, it } from "vitest";
import { workspaceBindingSchema } from "./serverWorkspaceSchemas";

describe("workspaceBindingSchema", () => {
  it("accepts changed local binding metadata without an absolute path", () => {
    const parsed = workspaceBindingSchema.parse({
      id: "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23",
      workspaceId: "527d6713-f2d4-4b0d-8f72-eaa7f19ebd24",
      deviceId: "627d6713-f2d4-4b0d-8f72-eaa7f19ebd25",
      deviceName: "Samuel's Mac",
      deviceStatus: "ONLINE",
      displayName: "nexo-ia",
      status: "CHANGED",
      structureFingerprint: "abc",
      gitHead: "deadbeef",
      gitBranch: "main",
      lastSeenAt: "2026-08-26T20:00:00Z",
      createdAt: "2026-08-26T19:59:00Z",
      updatedAt: "2026-08-26T20:00:00Z"
    });

    expect(parsed.status).toBe("CHANGED");
    expect(parsed).not.toHaveProperty("rootPath");
  });
});
