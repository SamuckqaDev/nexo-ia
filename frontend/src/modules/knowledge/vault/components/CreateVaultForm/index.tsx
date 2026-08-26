import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, X } from "@phosphor-icons/react";
import { useForm, type SubmitHandler } from "react-hook-form";
import { useEffect, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { useKnowledgeWorkspaces } from "../../hooks/useKnowledgeWorkspaces";
import { createVaultSchema } from "../../schemas/createVaultSchema";
import type { CreateVaultFormProps, CreateVaultValues } from "../../types/vaultTypes";
import { Actions, Form, Textarea, TextareaField } from "./styles";

export function CreateVaultForm({ ownerOptions, pending, onCreate, onCancel }: CreateVaultFormProps): ReactElement {
  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<CreateVaultValues>({
    resolver: zodResolver(createVaultSchema),
    defaultValues: { name: "", description: "", ownerTarget: "personal", scope: "personal", workspaceId: "" }
  });
  const { workspaces } = useKnowledgeWorkspaces();
  const scope = watch("scope");
  const ownerTarget = watch("ownerTarget");
  const submit: SubmitHandler<CreateVaultValues> = (values): void => onCreate(values);

  useEffect((): void => {
    if (ownerTarget.startsWith("team:") && scope !== "personal") {
      setValue("scope", "personal", { shouldValidate: true });
    }
  }, [ownerTarget, scope, setValue]);

  return (
    <Form onSubmit={handleSubmit(submit)}>
      <Input id="vault-name" label="Vault name" placeholder="Example: Product research" error={errors.name?.message} {...register("name")} />
      <TextareaField>
        <label htmlFor="vault-description">Purpose</label>
        <Textarea id="vault-description" placeholder="What should Nexo retrieve from this Vault?" aria-invalid={Boolean(errors.description)} {...register("description")} />
        {errors.description?.message && <span>{errors.description.message}</span>}
      </TextareaField>
      <Select
        id="vault-owner"
        label="Owner"
        helperText="Team-owned Vaults are shared with that Team and remain separate from your personal account."
        error={errors.ownerTarget?.message}
        options={ownerOptions}
        {...register("ownerTarget")}
      />
      <Select
        id="vault-scope"
        label="Visibility scope"
        helperText="Visibility does not grant retrieval access; every use is authorized again."
        options={[
          { label: "Personal", value: "personal" },
          { label: "Workspace", value: "workspace" }
        ]}
        disabled={ownerTarget.startsWith("team:")}
        {...register("scope")}
      />
      {scope === "workspace" && (
        <Select
          id="vault-workspace"
          label="Workspace"
          error={errors.workspaceId?.message}
          helperText={workspaces.data?.length === 0 ? "No workspaces yet." : undefined}
          options={(workspaces.data ?? []).map((workspace) => ({ label: workspace.name, value: workspace.id }))}
          {...register("workspaceId")}
        />
      )}
      <Actions><Button type="button" variant="outline" size="compact" icon={X} onClick={onCancel}>Cancel</Button><Button type="submit" size="compact" icon={Plus} disabled={pending}>{pending ? "Creating…" : "Create Vault"}</Button></Actions>
    </Form>
  );
}
