import type { ReactElement } from "react";
import { Key, LockKey } from "@phosphor-icons/react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { PASSWORD_REQUIREMENTS } from "../../../../../shared/security/schemas/passwordSchema";
import { useChangePasswordForm } from "../../hooks/useChangePasswordForm";
import { Description, Form, Header, Section, Title } from "./styles";

export function ChangePasswordForm(): ReactElement {
  const { form, mutation, submit } = useChangePasswordForm();
  const { register, formState: { errors } } = form;

  return (
    <Section aria-labelledby="change-password-title">
      <Header>
        <Title id="change-password-title">Password security</Title>
        <Description>
          Confirm your current password. Other connected sessions will be revoked after the change.
        </Description>
      </Header>
      <Form onSubmit={submit} noValidate>
        <Input
          id="currentPassword"
          label="Current password"
          icon={Key}
          type="password"
          autoComplete="current-password"
          error={errors.currentPassword?.message}
          {...register("currentPassword")}
        />
        <Input
          id="changedPassword"
          label="New password"
          icon={LockKey}
          type="password"
          autoComplete="new-password"
          helperText={PASSWORD_REQUIREMENTS}
          error={errors.newPassword?.message}
          {...register("newPassword")}
        />
        <Input
          id="changedPasswordConfirmation"
          label="Confirm new password"
          icon={LockKey}
          type="password"
          autoComplete="new-password"
          error={errors.passwordConfirmation?.message}
          {...register("passwordConfirmation")}
        />
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Changing password…" : "Change password"}
        </Button>
      </Form>
    </Section>
  );
}
