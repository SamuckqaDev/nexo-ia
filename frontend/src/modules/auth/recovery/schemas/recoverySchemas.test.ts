import { describe, expect, it } from "vitest";
import { forgotPasswordSchema } from "./forgotPasswordSchema";
import { resetPasswordSchema } from "./resetPasswordSchema";

describe("password recovery schemas", () => {
  it("validates the recovery email", () => {
    expect(forgotPasswordSchema.safeParse({ email: "invalid" }).success).toBe(false);
    expect(forgotPasswordSchema.safeParse({ email: "owner@nexo.local" }).success).toBe(true);
  });

  it("requires a strong matching password", () => {
    expect(resetPasswordSchema.safeParse({
      password: "Nexo123!",
      passwordConfirmation: "Nexo123!"
    }).success).toBe(true);
    expect(resetPasswordSchema.safeParse({
      password: "Nexo123!",
      passwordConfirmation: "Different123!"
    }).success).toBe(false);
  });
});
