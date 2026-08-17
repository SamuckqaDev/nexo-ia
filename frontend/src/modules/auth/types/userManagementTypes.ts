import type { z } from "zod";
import type { UseFormReturn } from "react-hook-form";
import type { createMemberSchema, managedUserSchema } from "../user/schemas/userManagementSchema";

export type ManagedUser = z.infer<typeof managedUserSchema>;
export type CreateMemberValues = z.infer<typeof createMemberSchema>;
export type UserStatus = ManagedUser["status"];
export type UserManagementResult = {
  users: ManagedUser[];
  form: UseFormReturn<CreateMemberValues>;
  isLoading: boolean;
  isCreating: boolean;
  updatingUserId: string | null;
  submit: ReturnType<UseFormReturn<CreateMemberValues>["handleSubmit"]>;
  changeStatus: (userId: string, status: UserStatus) => void;
  generatePassword: () => void;
};
export type MemberSessionsProps = { userId: string };
