import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { changePassword } from "../../shared/api/authApi";
import type {
  ChangePasswordFormResult,
  ChangePasswordFormValues
} from "../../types/authTypes";
import { changePasswordSchema } from "../schemas/changePasswordSchema";

export function useChangePasswordForm(): ChangePasswordFormResult {
  const showSnackbar: SnackbarState["show"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["show"] => state.show
  );
  const form: ChangePasswordFormResult["form"] = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: "", newPassword: "", passwordConfirmation: "" }
  });
  const mutation: ChangePasswordFormResult["mutation"] = useMutation({
    mutationFn: changePassword,
    onSuccess: (): void => {
      form.reset();
      showSnackbar("Password changed. Other sessions were revoked.", { variant: "success" });
    },
    onError: (error: Error): void => showSnackbar(error.message, { variant: "error" })
  });
  const submit: ChangePasswordFormResult["submit"] = form.handleSubmit(
    (values: ChangePasswordFormValues): void => mutation.mutate(values)
  );

  return { form, mutation, submit };
}
