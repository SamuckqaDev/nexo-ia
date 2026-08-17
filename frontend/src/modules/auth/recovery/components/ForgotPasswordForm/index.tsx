import type { ReactElement } from "react";
import { ArrowLeft, EnvelopeSimple } from "@phosphor-icons/react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import type { ForgotPasswordFormProps } from "../../../types/authTypes";
import { useForgotPasswordForm } from "../../hooks/useForgotPasswordForm";
import { Description, Eyebrow, Form, Intro, TextButton, Title } from "./styles";

export function ForgotPasswordForm({ onBackToLogin }: ForgotPasswordFormProps): ReactElement {
  const { form, mutation, submit } = useForgotPasswordForm();
  const { register, formState: { errors } } = form;

  return (
    <Form onSubmit={submit} noValidate>
      <Intro>
        <Eyebrow>Account recovery</Eyebrow>
        <Title>Reset your password</Title>
        <Description>Enter your account email and Nexo will send a secure, single-use link.</Description>
      </Intro>
      <Input
        id="recoveryEmail"
        label="Email"
        icon={EnvelopeSimple}
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email")}
      />
      <Button type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? "Sending instructions…" : "Send reset link"}
      </Button>
      <TextButton type="button" onClick={onBackToLogin}>
        <ArrowLeft aria-hidden size={16} /> Back to login
      </TextButton>
    </Form>
  );
}
