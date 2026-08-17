import { apiClient } from "../../../../shared/api/client";
import type {
  ForgotPasswordFormValues,
  ResetPasswordFormValues
} from "../../types/authTypes";
import { ensureCsrf } from "../../shared/api/authApi";

export function requestPasswordReset(input: ForgotPasswordFormValues): Promise<void> {
  return ensureCsrf()
    .then(() => apiClient.post("/auth/password/forgot", input))
    .then(() => undefined);
}

export function resetPassword(token: string, input: ResetPasswordFormValues): Promise<void> {
  return ensureCsrf()
    .then(() => apiClient.post("/auth/password/reset", { token, password: input.password }))
    .then(() => undefined);
}
