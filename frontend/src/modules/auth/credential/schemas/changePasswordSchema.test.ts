import { describe, expect, it } from "vitest";
import { changePasswordSchema } from "./changePasswordSchema";

describe("changePasswordSchema", () => {
  it("accepts a strong new password different from the current password", () => {
    expect(changePasswordSchema.safeParse({
      currentPassword: "Nexo123!",
      newPassword: "Nexo456!",
      passwordConfirmation: "Nexo456!"
    }).success).toBe(true);
  });

  it("rejects password reuse and mismatched confirmation", () => {
    expect(changePasswordSchema.safeParse({
      currentPassword: "Nexo123!",
      newPassword: "Nexo123!",
      passwordConfirmation: "Different456!"
    }).success).toBe(false);
  });
});
