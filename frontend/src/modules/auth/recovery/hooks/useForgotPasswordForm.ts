import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { requestPasswordReset } from "../api/recoveryApi";
import type {
  ForgotPasswordFormResult,
  ForgotPasswordFormValues
} from "../../types/authTypes";
import { forgotPasswordSchema } from "../schemas/forgotPasswordSchema";

export function useForgotPasswordForm(): ForgotPasswordFormResult {
  const showSnackbar: SnackbarState["show"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["show"] => state.show
  );
  const form: ForgotPasswordFormResult["form"] = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: "" }
  });
  const mutation: ForgotPasswordFormResult["mutation"] = useMutation({
    mutationFn: requestPasswordReset,
    onSuccess: (): void => {
      form.reset();
      showSnackbar("If the account exists, reset instructions were sent.", { variant: "success" });
    },
    onError: (error: Error): void => showSnackbar(error.message, { variant: "error" })
  });
  const submit: ForgotPasswordFormResult["submit"] = form.handleSubmit(
    (values: ForgotPasswordFormValues): void => mutation.mutate(values)
  );

  return { form, mutation, submit };
}
