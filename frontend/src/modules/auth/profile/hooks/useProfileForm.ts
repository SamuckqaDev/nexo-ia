import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult } from "@tanstack/react-query";
import type { BaseSyntheticEvent } from "react";
import { useForm, type UseFormReturn } from "react-hook-form";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import type { AuthenticatedUser } from "../../types/authTypes";
import { updateProfile } from "../api/profileApi";
import { updateProfileSchema, type UpdateProfileFormValues } from "../schemas/updateProfileSchema";

export type ProfileFormResult = {
  form: UseFormReturn<UpdateProfileFormValues>;
  mutation: UseMutationResult<AuthenticatedUser, Error, UpdateProfileFormValues>;
  submit: (event?: BaseSyntheticEvent) => Promise<void>;
};

export function useProfileForm(user: AuthenticatedUser): ProfileFormResult {
  const queryClient: QueryClient = useQueryClient();
  const showSnackbar: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const form: UseFormReturn<UpdateProfileFormValues> = useForm<UpdateProfileFormValues>({
    resolver: zodResolver(updateProfileSchema),
    defaultValues: {
      name: user.name,
      username: user.username,
      email: user.email,
      birthDate: user.birthDate
    }
  });
  const mutation: ProfileFormResult["mutation"] = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updatedUser: AuthenticatedUser): void => {
      queryClient.setQueryData(["auth", "session"], updatedUser);
      form.reset(updatedUser);
      showSnackbar("Profile updated.", { variant: "success" });
    },
    onError: (error: Error): void => showSnackbar(error.message, { variant: "error" })
  });
  const submit: ProfileFormResult["submit"] = form.handleSubmit(
    (values: UpdateProfileFormValues): void => mutation.mutate(values)
  );
  return { form, mutation, submit };
}
