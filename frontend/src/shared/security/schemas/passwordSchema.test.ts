import { describe, expect, it } from "vitest";
import { passwordSchema } from "./passwordSchema";

describe("passwordSchema", () => {
  it.each([
    ["short", "Use at least 8 characters"],
    ["password1!", "Add at least one uppercase letter"],
    ["PASSWORD1!", "Add at least one lowercase letter"],
    ["Password!", "Add at least one number"],
    ["Password1", "Add at least one special character"]
  ])("rejects an incomplete password policy", (password, message) => {
    const result = passwordSchema.safeParse(password);

    expect(result.success).toBe(false);
    expect(result.error?.issues.map((issue) => issue.message)).toContain(message);
  });

  it("accepts a composed password", () => {
    expect(passwordSchema.safeParse("Nexo123!").success).toBe(true);
  });
});
