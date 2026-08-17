import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import { createOwner } from "../../shared/api/authApi";
import { createOwnerSchema } from "../schemas/createOwnerSchema";
import type { CreateOwnerFormResult, CreateOwnerFormValues } from "../../types/authTypes";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";

export function useCreateOwnerForm(): CreateOwnerFormResult {
  const queryClient: QueryClient = useQueryClient();
  const showSnackbar: SnackbarState["show"] = useSnackbarStore(
    (state: SnackbarState): SnackbarState["show"] => state.show
  );
  const form: CreateOwnerFormResult["form"] = useForm<CreateOwnerFormValues>({
    resolver: zodResolver(createOwnerSchema),
    defaultValues: { name: "", username: "", email: "", password: "", passwordConfirmation: "" }
  });
  const mutation: CreateOwnerFormResult["mutation"] = useMutation({
    mutationFn: ({ passwordConfirmation: _, ...values }: CreateOwnerFormValues) => createOwner(values),
    onSuccess: () => {
      form.reset();
      showSnackbar("Owner account created successfully.", { variant: "success" });
      return queryClient.invalidateQueries({ queryKey: ["auth"] });
    },
    onError: (error) => showSnackbar(error.message, { variant: "error" })
  });

  const submit: CreateOwnerFormResult["submit"] = form.handleSubmit(
    (values: CreateOwnerFormValues): void => mutation.mutate(values)
  );

  return { form, mutation, submit };
}
