import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, X } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { useForm, type SubmitHandler } from "react-hook-form";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { createTeamSchema } from "../../schemas/teamSchemas";
import type { CreateTeamFormProps, CreateTeamValues } from "../../types/teamTypes";
import { Actions, Form } from "./styles";

export function CreateTeamForm({ pending, onSubmit, onCancel }: CreateTeamFormProps): ReactElement {
  const { register, handleSubmit, formState: { errors } } = useForm<CreateTeamValues>({
    resolver: zodResolver(createTeamSchema),
    defaultValues: { name: "", defaultProfile: "RESEARCHER", tokenBudgetLimit: undefined }
  });
  const submit: SubmitHandler<CreateTeamValues> = (values): void => onSubmit(values);

  return (
    <Form onSubmit={handleSubmit(submit)}>
      <Input id="team-name" label="Team name" placeholder="Example: Platform engineering" error={errors.name?.message} {...register("name")} />
      <Select
        id="team-default-profile"
        label="Default capability profile"
        helperText="Applied to new members unless an administrator chooses another profile."
        options={[
          { label: "Locked — no execution", value: "LOCKED" },
          { label: "Reader — knowledge only", value: "READER" },
          { label: "Researcher — read tools", value: "RESEARCHER" },
          { label: "Builder — project changes", value: "BUILDER" },
          { label: "Operator — broad operation", value: "OPERATOR" }
        ]}
        {...register("defaultProfile")}
      />
      <Input
        id="team-budget"
        type="number"
        min={1}
        label="Token budget (optional)"
        helperText="Stored as the Team allocation. Runtime enforcement is shown only when active Team context is enabled."
        error={errors.tokenBudgetLimit?.message}
        placeholder="100000"
        {...register("tokenBudgetLimit", {
          setValueAs: (value: string): number | undefined => value === "" ? undefined : Number(value)
        })}
      />
      <Actions>
        <Button type="button" variant="outline" size="compact" icon={X} onClick={onCancel}>Cancel</Button>
        <Button type="submit" size="compact" icon={Plus} disabled={pending}>{pending ? "Creating…" : "Create Team"}</Button>
      </Actions>
    </Form>
  );
}
