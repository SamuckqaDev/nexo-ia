import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, X } from "@phosphor-icons/react";
import { useForm, type SubmitHandler } from "react-hook-form";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { useKnowledgeWorkspaces } from "../../hooks/useKnowledgeWorkspaces";
import { createVaultSchema } from "../../schemas/createVaultSchema";
import type { CreateVaultFormProps, CreateVaultValues } from "../../types/vaultTypes";
import { Actions, Form, Textarea, TextareaField } from "./styles";

export function CreateVaultForm({ onCreate, onCancel }: CreateVaultFormProps): ReactElement {
  const { register, handleSubmit, watch, formState: { errors } } = useForm<CreateVaultValues>({
    resolver: zodResolver(createVaultSchema),
    defaultValues: { name: "", description: "", scope: "personal", workspaceId: "" }
  });
  const { workspaces } = useKnowledgeWorkspaces();
  const scope = watch("scope");
  const submit: SubmitHandler<CreateVaultValues> = (values): void => onCreate(values);

  return (
    <Form onSubmit={handleSubmit(submit)}>
      <Input id="vault-name" label="Vault name" placeholder="Example: Product research" error={errors.name?.message} {...register("name")} />
      <TextareaField>
        <label htmlFor="vault-description">Purpose</label>
        <Textarea id="vault-description" placeholder="What should Nexo retrieve from this Vault?" aria-invalid={Boolean(errors.description)} {...register("description")} />
        {errors.description?.message && <span>{errors.description.message}</span>}
      </TextareaField>
      <Select
        id="vault-scope"
        label="Visibility scope"
        helperText="Visibility does not grant retrieval access; every use is authorized again."
        options={[
          { label: "Personal", value: "personal" },
          { label: "Workspace", value: "workspace" },
          { label: "Project", value: "project" },
          { label: "Team", value: "team" },
          { label: "Organization", value: "organization" }
        ]}
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
      <Actions><Button type="button" variant="outline" icon={X} onClick={onCancel}>Cancel</Button><Button type="submit" icon={Plus}>Create draft</Button></Actions>
    </Form>
  );
}
