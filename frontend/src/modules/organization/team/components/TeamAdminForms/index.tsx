import { zodResolver } from "@hookform/resolvers/zod";
import { BookOpen, UserPlus } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { useForm, type SubmitHandler } from "react-hook-form";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { addTeamMemberSchema, createTeamVaultSchema } from "../../schemas/teamSchemas";
import type {
  AddTeamMemberValues,
  CreateTeamVaultValues,
  TeamAdminFormsProps
} from "../../types/teamTypes";
import { AdminCopy, AdminGrid, Actions, Form, Textarea } from "./styles";

export function TeamAdminForms({
  candidates,
  canAppointAdmin,
  memberPending,
  vaultPending,
  onAddMember,
  onCreateVault
}: TeamAdminFormsProps): ReactElement {
  const memberForm = useForm<AddTeamMemberValues>({
    resolver: zodResolver(addTeamMemberSchema),
    defaultValues: { userId: "", teamRole: "MEMBER", profile: "RESEARCHER" }
  });
  const vaultForm = useForm<CreateTeamVaultValues>({
    resolver: zodResolver(createTeamVaultSchema),
    defaultValues: { name: "", description: "" }
  });
  const addMember: SubmitHandler<AddTeamMemberValues> = (values): void => onAddMember(values);
  const createVault: SubmitHandler<CreateTeamVaultValues> = (values): void => onCreateVault(values);

  return (
    <AdminGrid>
      <Form onSubmit={memberForm.handleSubmit(addMember)}>
        <AdminCopy><h4>Add a member</h4><p>Grant an existing Nexo user access to this Team.</p></AdminCopy>
        <Select
          id="team-candidate"
          label="Nexo user"
          error={memberForm.formState.errors.userId?.message}
          options={[
            { label: candidates.length ? "Choose a user" : "No users available", value: "" },
            ...candidates.map((candidate) => ({
              label: `${candidate.name} · ${candidate.email}`,
              value: candidate.userId
            }))
          ]}
          {...memberForm.register("userId")}
        />
        <Select
          id="team-member-role"
          label="Team role"
          helperText={canAppointAdmin
            ? "The system Owner may appoint another Team administrator."
            : "Only the system Owner may appoint Team administrators."}
          options={[
            { label: "Member", value: "MEMBER" },
            ...(canAppointAdmin ? [{ label: "Administrator", value: "ADMIN" as const }] : [])
          ]}
          {...memberForm.register("teamRole")}
        />
        <Select
          id="team-member-profile"
          label="Capability profile"
          options={[
            { label: "Locked", value: "LOCKED" },
            { label: "Reader", value: "READER" },
            { label: "Researcher", value: "RESEARCHER" },
            { label: "Builder", value: "BUILDER" },
            { label: "Operator", value: "OPERATOR" }
          ]}
          {...memberForm.register("profile")}
        />
        <Actions><Button type="submit" size="compact" icon={UserPlus} disabled={memberPending || !candidates.length}>{memberPending ? "Adding…" : "Add member"}</Button></Actions>
      </Form>

      <Form onSubmit={vaultForm.handleSubmit(createVault)}>
        <AdminCopy><h4>Create shared knowledge</h4><p>Every member can retrieve this Vault; Team administrators manage its sources.</p></AdminCopy>
        <Input id="team-vault-name" label="Vault name" placeholder="Example: Engineering handbook" error={vaultForm.formState.errors.name?.message} {...vaultForm.register("name")} />
        <label htmlFor="team-vault-description">Purpose</label>
        <Textarea id="team-vault-description" placeholder="What shared knowledge belongs here?" aria-invalid={Boolean(vaultForm.formState.errors.description)} {...vaultForm.register("description")} />
        {vaultForm.formState.errors.description?.message && <small>{vaultForm.formState.errors.description.message}</small>}
        <Actions><Button type="submit" size="compact" icon={BookOpen} disabled={vaultPending}>{vaultPending ? "Creating…" : "Create Team Vault"}</Button></Actions>
      </Form>
    </AdminGrid>
  );
}
