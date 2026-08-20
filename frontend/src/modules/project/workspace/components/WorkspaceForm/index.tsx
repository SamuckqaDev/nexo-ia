import { zodResolver } from "@hookform/resolvers/zod";
import { FolderSimplePlus, X } from "@phosphor-icons/react";
import { useForm, type SubmitHandler } from "react-hook-form";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { addWorkspaceSchema } from "../../schemas/addWorkspaceSchema";
import { useWorkspaceStore } from "../../stores/useWorkspaceStore";
import type { AddWorkspaceValues, ProjectWorkspace, WorkspaceFormProps, WorkspaceState } from "../../types/workspaceTypes";
import { Actions, Form, Notice } from "./styles";

export function WorkspaceForm({ onAdded, onCancel }: WorkspaceFormProps): ReactElement {
  const addWorkspace: WorkspaceState["addWorkspace"] = useWorkspaceStore((state: WorkspaceState) => state.addWorkspace);
  const { register, handleSubmit, formState: { errors } } = useForm<AddWorkspaceValues>({
    resolver: zodResolver(addWorkspaceSchema),
    defaultValues: { name: "", path: "", access: "read" }
  });
  const submit: SubmitHandler<AddWorkspaceValues> = (values): void => {
    const workspace: ProjectWorkspace = addWorkspace(values);
    onAdded(workspace);
  };

  return (
    <Form onSubmit={handleSubmit(submit)}>
      <Input id="workspace-name" label="Workspace name" placeholder="Example: Nexo IA" error={errors.name?.message} {...register("name")} />
      <Input
        id="workspace-path"
        label="Project folder"
        placeholder="/home/user/projects/nexo-ia"
        helperText="Use the exact root you want Nexo to treat as this project's working directory."
        error={errors.path?.message}
        {...register("path")}
      />
      <Select
        id="workspace-access"
        label="Initial session scope"
        helperText="This frontend choice is not an authoritative permission grant."
        options={[
          { label: "Read-only inspection", value: "read" },
          { label: "Read and propose edits", value: "propose" },
          { label: "Commands require approval", value: "commands" }
        ]}
        {...register("access")}
      />
      <Notice>Folder selection is kept only for this browser session. The Companion must canonicalize and authorize the path before Nexo reads any file.</Notice>
      <Actions>
        <Button type="button" variant="outline" icon={X} onClick={onCancel}>Cancel</Button>
        <Button type="submit" icon={FolderSimplePlus}>Add and select</Button>
      </Actions>
    </Form>
  );
}
