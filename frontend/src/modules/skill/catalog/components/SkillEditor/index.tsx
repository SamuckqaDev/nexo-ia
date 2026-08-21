import { zodResolver } from "@hookform/resolvers/zod";
import { FloppyDisk, ShieldCheck } from "@phosphor-icons/react";
import { useForm, type SubmitHandler } from "react-hook-form";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { skillEditorSchema } from "../../schemas/skillEditorSchema";
import type { SkillEditorProps, SkillEditorValues } from "../../types/skillTypes";
import { useActiveWorkspace } from "../../../../project/workspace/hooks/useActiveWorkspace";
import { useWorkspaceStore } from "../../../../project/workspace/stores/useWorkspaceStore";
import type { ProjectWorkspace, WorkspaceState } from "../../../../project/workspace/types/workspaceTypes";
import { EditorForm, Field, FormActions, Notice, Textarea, TwoColumns } from "./styles";

export function SkillEditor({ initialSkill, onSave }: SkillEditorProps): ReactElement {
  const activeWorkspace = useActiveWorkspace();
  const workspaces: ProjectWorkspace[] = useWorkspaceStore((state: WorkspaceState) => state.workspaces);
  const { register, handleSubmit, watch, formState: { errors } } = useForm<SkillEditorValues>({
    resolver: zodResolver(skillEditorSchema),
    defaultValues: {
      name: initialSkill?.name ?? "",
      description: initialSkill?.description ?? "",
      scope: initialSkill?.scope === "built_in" ? "personal" : initialSkill?.scope ?? "personal",
      activation: initialSkill?.activation ?? "explicit",
      instructions: initialSkill?.instructions ?? "",
      outputContract: initialSkill?.outputContract ?? "",
      dependencies: initialSkill?.dependencies.join(", ") ?? "",
      scopeTarget: initialSkill?.scopeTarget ?? (initialSkill?.scope === "project" ? activeWorkspace?.id : "")
    }
  });
  const scope = watch("scope");
  const submit: SubmitHandler<SkillEditorValues> = (values): void => onSave(values, initialSkill?.preview ? undefined : initialSkill?.id);

  return (
    <EditorForm onSubmit={handleSubmit(submit)}>
      <TwoColumns>
        <Input id="skill-name" label="Skill name" placeholder="Example: Review a pull request" error={errors.name?.message} {...register("name")} />
        <Select
          id="skill-scope"
          label="Ownership scope"
          options={[
            { label: "Personal", value: "personal" },
            { label: "Project", value: "project" },
            { label: "Workspace", value: "workspace" },
            { label: "Team", value: "team" },
            { label: "Organization", value: "organization" },
            { label: "Session only", value: "session" }
          ]}
          {...register("scope")}
        />
      </TwoColumns>
      {(scope === "project" || scope === "team") && (
        scope === "project" ? (
          <Select
            id="skill-scope-target"
            label="Project"
            helperText="Choose the project/workspace that owns this Skill."
            options={[{ label: "Select a project", value: "" }, ...workspaces.map((workspace: ProjectWorkspace) => ({ label: `${workspace.name} · ${workspace.directoryName}`, value: workspace.id }))]}
            error={errors.scopeTarget?.message}
            {...register("scopeTarget")}
          />
        ) : (
          <Input id="skill-scope-target" label="Team" placeholder="Example: Platform team" helperText="Skills are shared only with this team scope." error={errors.scopeTarget?.message} {...register("scopeTarget")} />
        )
      )}
      <Input id="skill-description" label="When should Nexo use it?" placeholder="Describe the task pattern and boundaries" error={errors.description?.message} {...register("description")} />
      <Select
        id="skill-activation"
        label="Activation"
        helperText="Suggested activation may recommend the Skill; it never bypasses permission filters."
        options={[{ label: "Explicit invocation only", value: "explicit" }, { label: "May be suggested when relevant", value: "suggested" }]}
        {...register("activation")}
      />
      <Field>
        <label htmlFor="skill-instructions">Workflow instructions</label>
        <Textarea id="skill-instructions" placeholder={'1. Inspect the relevant context\n2. Apply the method\n3. Verify the output'} $large aria-invalid={Boolean(errors.instructions)} {...register("instructions")} />
        <small>Write the method, decision points, safety boundaries and verification steps.</small>
        {errors.instructions?.message && <span>{errors.instructions.message}</span>}
      </Field>
      <Field>
        <label htmlFor="skill-output">Expected output</label>
        <Textarea id="skill-output" placeholder="A concise report with findings, evidence and next actions." aria-invalid={Boolean(errors.outputContract)} {...register("outputContract")} />
        {errors.outputContract?.message && <span>{errors.outputContract.message}</span>}
      </Field>
      <Input id="skill-dependencies" label="Declared dependencies" placeholder="Vault: product docs, Tool: filesystem.read" helperText="Comma-separated. Dependencies are declarations, never inherited permissions." {...register("dependencies")} />
      <Notice><ShieldCheck size={18} weight="duotone" /><span>The draft stores workflow knowledge only. Vaults, tools, providers and secrets must be authorized again for every principal and run.</span></Notice>
      <FormActions><Button type="submit" icon={FloppyDisk}>{initialSkill && !initialSkill.preview ? "Update draft" : "Save Skill draft"}</Button></FormActions>
    </EditorForm>
  );
}
