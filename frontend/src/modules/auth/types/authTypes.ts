import type { z } from "zod";
import type { BaseSyntheticEvent } from "react";
import type { UseFormReturn } from "react-hook-form";
import type { UseMutationResult } from "@tanstack/react-query";
import type { createOwnerSchema } from "../bootstrap/schemas/createOwnerSchema";
import type { loginSchema } from "../session/schemas/loginSchema";
import type { userSchema } from "../shared/schemas/authSchemas";
import type { forgotPasswordSchema } from "../recovery/schemas/forgotPasswordSchema";
import type { resetPasswordSchema } from "../recovery/schemas/resetPasswordSchema";
import type { changePasswordSchema } from "../credential/schemas/changePasswordSchema";

export type CreateOwnerFormValues = z.infer<typeof createOwnerSchema>;
export type LoginFormValues = z.infer<typeof loginSchema>;
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
export type AuthenticatedUser = z.infer<typeof userSchema>;
export type CreateOwnerRequest = Omit<CreateOwnerFormValues, "passwordConfirmation">;

export type CreateOwnerFormResult = {
  form: UseFormReturn<CreateOwnerFormValues>;
  mutation: UseMutationResult<AuthenticatedUser, Error, CreateOwnerFormValues>;
  submit: (event?: BaseSyntheticEvent) => Promise<void>;
};

export type LoginFormResult = {
  form: UseFormReturn<LoginFormValues>;
  mutation: UseMutationResult<AuthenticatedUser, Error, LoginFormValues>;
  submit: (event?: BaseSyntheticEvent) => Promise<void>;
};

export type ForgotPasswordFormResult = {
  form: UseFormReturn<ForgotPasswordFormValues>;
  mutation: UseMutationResult<void, Error, ForgotPasswordFormValues>;
  submit: (event?: BaseSyntheticEvent) => Promise<void>;
};

export type ResetPasswordFormResult = {
  form: UseFormReturn<ResetPasswordFormValues>;
  mutation: UseMutationResult<void, Error, ResetPasswordFormValues>;
  submit: (event?: BaseSyntheticEvent) => Promise<void>;
};

export type ChangePasswordFormResult = {
  form: UseFormReturn<ChangePasswordFormValues>;
  mutation: UseMutationResult<void, Error, ChangePasswordFormValues>;
  submit: (event?: BaseSyntheticEvent) => Promise<void>;
};

export type AuthView = "login" | "forgot-password" | "reset-password";

export type LoginFormProps = {
  onForgotPassword: () => void;
};

export type ForgotPasswordFormProps = {
  onBackToLogin: () => void;
};

export type ResetPasswordFormProps = {
  token: string;
  onResetCompleted: () => void;
};

export type AuthSessionResult = {
  bootstrapRequired: boolean | undefined;
  user: AuthenticatedUser | null | undefined;
  isLoading: boolean;
  error: Error | null;
  logout: () => void;
  isLoggingOut: boolean;
};

export type AuthPageProps = {
  bootstrapRequired: boolean;
};

export type ProfilePanelProps = {
  user: AuthenticatedUser;
  onLogout: () => void;
  isLoggingOut: boolean;
};
