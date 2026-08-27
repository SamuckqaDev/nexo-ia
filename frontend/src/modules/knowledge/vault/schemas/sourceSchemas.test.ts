import { describe, expect, it } from "vitest";
import { backendSourceSchema } from "./sourceSchemas";

const source = {
  id: "22222222-2222-4222-8222-222222222222",
  vaultId: "11111111-1111-4111-8111-111111111111",
  displayName: "Architecture decision",
  mimeType: "text/markdown",
  byteSize: 128,
  status: "READY",
  errorCode: null,
  createdAt: "2026-08-27T00:00:00Z",
  updatedAt: "2026-08-27T00:00:00Z"
};

describe("backendSourceSchema", () => {
  it.each(["UPLOAD", "AGENT"] as const)("accepts %s sources returned by the backend", (sourceKind) => {
    expect(backendSourceSchema.parse({ ...source, sourceKind }).sourceKind).toBe(sourceKind);
  });
});
