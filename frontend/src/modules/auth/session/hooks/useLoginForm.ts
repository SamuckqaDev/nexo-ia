import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import { login } from "../../shared/api/authApi";
import { loginSchema } from "../schemas/loginSchema";
import type { LoginFormResult, LoginFormValues } from "../../types/authTypes";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";

export function useLoginForm(): LoginFormResult {
  const queryClient: QueryClient = useQueryClient();
  const showSnackbar: SnackbarState["show"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["show"] => state.show
  );
  const form: LoginFormResult["form"] = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identifier: "", password: "" }
  });
  const mutation: LoginFormResult["mutation"] = useMutation({
    mutationFn: login,
    onSuccess: () => {
      if (import.meta.env.DEV) console.info("[NEXO-FRONT][AUTH] Login succeeded");
      form.reset();
      showSnackbar("Welcome back to Nexo IA.", { variant: "success" });
      return queryClient.invalidateQueries({ queryKey: ["auth"] });
    },
    onError: (error) => {
      if (import.meta.env.DEV) console.warn("[NEXO-FRONT][AUTH] Login failed", error.message);
      showSnackbar(error.message, { variant: "error" });
    }
  });

  const submit: LoginFormResult["submit"] = form.handleSubmit(
    (values: LoginFormValues): void => mutation.mutate(values)
  );

  return { form, mutation, submit };
}
