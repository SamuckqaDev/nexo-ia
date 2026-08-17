import type { ReactElement } from "react";
import { At, LockKey } from "@phosphor-icons/react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { useLoginForm } from "../../hooks/useLoginForm";
import type { LoginFormProps } from "../../../types/authTypes";
import { Description, Eyebrow, Form, Intro, TextButton, Title } from "./styles";

export function LoginForm({ onForgotPassword }: LoginFormProps): ReactElement {
  const { form, mutation, submit } = useLoginForm();
  const { register, formState: { errors } } = form;

  return (
    <Form onSubmit={submit} noValidate>
      <Intro>
        <Eyebrow>Private workspace</Eyebrow>
        <Title>Welcome back</Title>
        <Description>Connect to your governed Nexo workspace.</Description>
      </Intro>
      <Input
        id="identifier"
        label="Username or email"
        icon={At}
        autoComplete="username"
        error={errors.identifier?.message}
        {...register("identifier")}
      />
      <Input
        id="loginPassword"
        label="Password"
        icon={LockKey}
        type="password"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register("password")}
      />
      <TextButton type="button" onClick={onForgotPassword}>
        Forgot your password?
      </TextButton>
      <Button type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? "Connecting…" : "Enter Nexo"}
      </Button>
    </Form>
  );
}
