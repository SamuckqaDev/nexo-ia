import { z } from "zod";
import { apiClient } from "../../../../shared/api/client";
import { clearAuthenticatedSession, hasAuthenticatedSession, markAuthenticatedSession } from "../../../../shared/auth/authState";
import { useSessionExpiredStore } from "../../../../shared/auth/sessionExpiredStore";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import type {
  AuthenticatedUser,
  ChangePasswordFormValues,
  CreateOwnerRequest,
  LoginFormValues,
} from "../../types/authTypes";
import { bootstrapStatusSchema, userSchema } from "../schemas/authSchemas";

const first = <T>(response: BaseResponse<T>): T => {
  const value: T | undefined = response.data?.[0];
  if (!value) throw new Error("Nexo returned an empty authentication response");
  return value;
};

export function ensureCsrf(): Promise<void> {
  return apiClient.get("/auth/csrf").then(() => undefined);
}

export function getBootstrapStatus(): Promise<boolean> {
  return apiClient.get<BaseResponse<unknown>>("/auth/bootstrap")
    .then(({ data }) => bootstrapStatusSchema.parse(first(data)).required);
}

export function getCurrentUser(): Promise<AuthenticatedUser | null> {
  return apiClient.get<BaseResponse<unknown>>("/auth/me")
    .then(({ data }) => userSchema.parse(first(data)))
    .then((user) => { markAuthenticatedSession(); return user; })
    .catch((error: unknown) => {
      if (typeof error === "object" && error !== null && "status" in error && error.status === 401) {
        if (hasAuthenticatedSession()) {
          useSessionExpiredStore.getState().open();
          return Promise.reject(error);
        }
        return null;
      }
      return Promise.reject(error);
    });
}

export function createOwner(input: CreateOwnerRequest): Promise<AuthenticatedUser> {
  return ensureCsrf()
    .then(() => apiClient.post<BaseResponse<unknown>>("/auth/bootstrap", input))
    .then(({ data }) => userSchema.parse(first(data)))
    .then((user) => { markAuthenticatedSession(); return user; });
}

export function login(input: LoginFormValues): Promise<AuthenticatedUser> {
  return ensureCsrf()
    .then(() => apiClient.post<BaseResponse<unknown>>("/auth/login", input))
    .then(({ data }) => userSchema.parse(first(data)))
    .then((user) => { markAuthenticatedSession(); return user; });
}

export function logout(): Promise<void> {
  return ensureCsrf()
    .then(() => apiClient.post("/auth/logout"))
    .then(() => { clearAuthenticatedSession(); });
}

export function changePassword(input: ChangePasswordFormValues): Promise<void> {
  return ensureCsrf()
    .then(() => apiClient.put("/auth/password", {
      currentPassword: input.currentPassword,
      newPassword: input.newPassword
    }))
    .then(() => undefined);
}
