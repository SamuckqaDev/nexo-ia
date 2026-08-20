import { FolderSimplePlus, HardDrive, X } from "@phosphor-icons/react";
import { useForm, type UseFormReturn } from "react-hook-form";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Select } from "../../../../../shared/components/Select";
import { addWorkspaceSchema } from "../../schemas/addWorkspaceSchema";
import { useWorkspaceRegistration } from "../../hooks/useWorkspaceRegistration";
import { workspacePlatformLabel } from "../../services/workspacePlatformService";
import type { AddWorkspaceValues, ProjectWorkspace, WorkspaceFormProps } from "../../types/workspaceTypes";
import { Actions, ErrorMessage, Form, Notice, Picker } from "./styles";

export function WorkspaceForm({ onAdded, onCancel }: WorkspaceFormProps): ReactElement {
  const registration = useWorkspaceRegistration();
  const form: UseFormReturn<AddWorkspaceValues> = useForm<AddWorkspaceValues>({
    defaultValues: { access: "read" }
  });

  const chooseFolder = (): void => {
    const values: AddWorkspaceValues = addWorkspaceSchema.parse(form.getValues());
    registration.chooseFolder(values.access)
      .then((workspace: ProjectWorkspace | null): void => {
        if (workspace) onAdded(workspace);
      });
  };

  return (
    <Form>
      <Picker>
        <span><HardDrive size={27} weight="duotone" /></span>
        <div>
          <strong>Select the real project folder</strong>
          <p>Nexo will open the {workspacePlatformLabel(registration.platform)} folder chooser and keep the granted directory handle only on this device.</p>
        </div>
        <Button
          type="button"
          icon={FolderSimplePlus}
          disabled={!registration.isSupported || registration.isPicking}
          onClick={chooseFolder}
        >
          {registration.isPicking ? "Reading project structure…" : registration.actionLabel}
        </Button>
      </Picker>
      <Select
        id="workspace-access"
        label="Initial session scope"
        helperText="Folder monitoring is read-only. Editing and commands still require their governed runtime and approvals."
        options={[
          { label: "Read-only inspection", value: "read" },
          { label: "Read and propose edits", value: "propose" },
          { label: "Commands require approval", value: "commands" }
        ]}
        {...form.register("access")}
      />
      {!registration.isSupported && (
        <ErrorMessage role="alert">
          Persistent folder access requires Chrome or Edge on HTTPS or localhost. Firefox and Safari cannot yet save a reusable directory handle.
        </ErrorMessage>
      )}
      {registration.error && <ErrorMessage role="alert">{registration.error}</ErrorMessage>}
      <Notice>
        The workspace is isolated to your Nexo account in this browser. The absolute path stays private; Nexo stores the browser-managed folder handle and a bounded metadata snapshot, never file contents.
      </Notice>
      <Actions>
        <Button type="button" variant="outline" icon={X} onClick={onCancel}>Cancel</Button>
      </Actions>
    </Form>
  );
}
