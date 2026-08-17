import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import { generateSecurePassword } from "../../../../shared/security/utils/generateSecurePassword";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import type { CreateMemberValues, UserManagementResult, UserStatus } from "../../types/userManagementTypes";
import { createMember, getManagedUsers, updateUserStatus } from "../api/userManagementApi";
import { createMemberSchema } from "../schemas/userManagementSchema";

const usersKey = ["admin", "users"] as const;

export function useUserManagement(): UserManagementResult {
  const client: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const form = useForm<CreateMemberValues>({
    resolver: zodResolver(createMemberSchema),
    defaultValues: { name: "", username: "", email: "", password: "" }
  });
  const query = useQuery({ queryKey: usersKey, queryFn: getManagedUsers });
  const createMutation = useMutation({
    mutationFn: createMember,
    onSuccess: (): void => { form.reset(); client.invalidateQueries({ queryKey: usersKey }); show("Member created.", { variant: "success" }); },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });
  const statusMutation = useMutation({
    mutationFn: ({ userId, status }: { userId: string; status: UserStatus }) => updateUserStatus(userId, status),
    onSuccess: (): void => { client.invalidateQueries({ queryKey: usersKey }); show("User access updated.", { variant: "success" }); },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });
  const submit = form.handleSubmit((values: CreateMemberValues): void => createMutation.mutate(values));
  const changeStatus = (userId: string, status: UserStatus): void => statusMutation.mutate({ userId, status });
  const generatePassword = (): void => {
    form.setValue("password", generateSecurePassword(), { shouldDirty: true, shouldValidate: true });
    show("A secure temporary password was generated.", { variant: "info" });
  };

  return { users: query.data ?? [], form, isLoading: query.isLoading,
    isCreating: createMutation.isPending, updatingUserId: statusMutation.variables?.userId ?? null,
    submit, changeStatus, generatePassword };
}
