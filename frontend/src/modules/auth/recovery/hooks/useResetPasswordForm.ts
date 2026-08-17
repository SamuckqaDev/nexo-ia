import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { resetPassword } from "../api/recoveryApi";
import type {
  ResetPasswordFormResult,
  ResetPasswordFormValues
} from "../../types/authTypes";
import { resetPasswordSchema } from "../schemas/resetPasswordSchema";

export function useResetPasswordForm(token: string, onCompleted: () => void): ResetPasswordFormResult {
  const showSnackbar: SnackbarState["show"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["show"] => state.show
  );
  const form: ResetPasswordFormResult["form"] = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { password: "", passwordConfirmation: "" }
  });
  const mutation: ResetPasswordFormResult["mutation"] = useMutation({
    mutationFn: (values: ResetPasswordFormValues): Promise<void> => resetPassword(token, values),
    onSuccess: (): void => {
      form.reset();
      showSnackbar("Password reset successfully. Sign in with your new password.", { variant: "success" });
      onCompleted();
    },
    onError: (error: Error): void => showSnackbar(error.message, { variant: "error" })
  });
  const submit: ResetPasswordFormResult["submit"] = form.handleSubmit(
    (values: ResetPasswordFormValues): void => mutation.mutate(values)
  );

  return { form, mutation, submit };
}
