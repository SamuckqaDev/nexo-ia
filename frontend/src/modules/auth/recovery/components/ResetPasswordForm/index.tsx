import type { ReactElement } from "react";
import { ArrowLeft, LockKey } from "@phosphor-icons/react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { PASSWORD_REQUIREMENTS } from "../../../../../shared/security/schemas/passwordSchema";
import type { ResetPasswordFormProps } from "../../../types/authTypes";
import { useResetPasswordForm } from "../../hooks/useResetPasswordForm";
import { Description, Eyebrow, Form, Intro, TextButton, Title } from "./styles";

export function ResetPasswordForm({ token, onResetCompleted }: ResetPasswordFormProps): ReactElement {
  const { form, mutation, submit } = useResetPasswordForm(token, onResetCompleted);
  const { register, formState: { errors } } = form;

  return (
    <Form onSubmit={submit} noValidate>
      <Intro>
        <Eyebrow>Secure recovery</Eyebrow>
        <Title>Choose a new password</Title>
        <Description>The link can be used only once and expires after 20 minutes.</Description>
      </Intro>
      <Input
        id="newPassword"
        label="New password"
        icon={LockKey}
        type="password"
        autoComplete="new-password"
        helperText={PASSWORD_REQUIREMENTS}
        error={errors.password?.message}
        {...register("password")}
      />
      <Input
        id="newPasswordConfirmation"
        label="Confirm new password"
        icon={LockKey}
        type="password"
        autoComplete="new-password"
        error={errors.passwordConfirmation?.message}
        {...register("passwordConfirmation")}
      />
      <Button type="submit" disabled={mutation.isPending || !token}>
        {mutation.isPending ? "Resetting password…" : "Reset password"}
      </Button>
      <TextButton type="button" onClick={onResetCompleted}>
        <ArrowLeft aria-hidden size={16} /> Back to login
      </TextButton>
    </Form>
  );
}
