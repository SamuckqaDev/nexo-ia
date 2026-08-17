import type { ReactElement } from "react";
import { At, EnvelopeSimple, IdentificationCard, LockKey } from "@phosphor-icons/react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { PASSWORD_REQUIREMENTS } from "../../../../../shared/security/schemas/passwordSchema";
import { useCreateOwnerForm } from "../../hooks/useCreateOwnerForm";
import { Description, Eyebrow, Form, Intro, Title } from "./styles";

export function CreateOwnerForm(): ReactElement {
  const { form, mutation, submit } = useCreateOwnerForm();
  const { register, formState: { errors } } = form;

  return (
    <Form onSubmit={submit} noValidate>
      <Intro>
        <Eyebrow>First connection</Eyebrow>
        <Title>Create the Nexo Owner</Title>
        <Description>This account governs this installation. It is not your operating-system root account.</Description>
      </Intro>
      <Input
        id="name"
        label="Name"
        icon={IdentificationCard}
        autoComplete="name"
        error={errors.name?.message}
        {...register("name")}
      />
      <Input
        id="username"
        label="Username"
        icon={At}
        autoComplete="username"
        error={errors.username?.message}
        {...register("username")}
      />
      <Input
        id="email"
        label="Email"
        icon={EnvelopeSimple}
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email")}
      />
      <Input
        id="password"
        label="Password"
        icon={LockKey}
        type="password"
        autoComplete="new-password"
        helperText={PASSWORD_REQUIREMENTS}
        error={errors.password?.message}
        {...register("password")}
      />
      <Input
        id="passwordConfirmation"
        label="Confirm password"
        icon={LockKey}
        type="password"
        autoComplete="new-password"
        error={errors.passwordConfirmation?.message}
        {...register("passwordConfirmation")}
      />
      <Button type="submit" disabled={mutation.isPending}>
        {mutation.isPending ? "Creating secure account…" : "Create Owner"}
      </Button>
    </Form>
  );
}
