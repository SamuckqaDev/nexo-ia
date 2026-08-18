import { At, CalendarBlank, Envelope, PencilSimple, User, X } from "@phosphor-icons/react";
import { useEffect, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import type { ProfileFormProps } from "../../../types/profileTypes";
import { useProfileForm } from "../../hooks/useProfileForm";
import { calculateAge } from "../../utils/profileAge";
import { Actions, AgeField, Form } from "./styles";

export function ProfileForm({ user }: ProfileFormProps): ReactElement {
  const [editing, setEditing] = useState<boolean>(false);
  const { form, mutation, submit } = useProfileForm(user);
  const { register, formState: { errors } } = form;
  const resetProfile = (): void => form.reset({
    name: user.name,
    username: user.username,
    email: user.email,
    birthDate: user.birthDate
  });
  const age: number | null = calculateAge(form.watch("birthDate"));

  useEffect((): void => {
    if (mutation.isSuccess) setEditing(false);
  }, [mutation.isSuccess]);

  return (
    <Form onSubmit={submit} noValidate>
      <Input id="profile-name" label="Name" icon={User} disabled={!editing} error={errors.name?.message} {...register("name")} />
      <Input id="profile-username" label="Username" icon={At} disabled={!editing} autoComplete="username" error={errors.username?.message} {...register("username")} />
      <Input id="profile-email" label="Email" icon={Envelope} disabled={!editing} type="email" autoComplete="email" error={errors.email?.message} {...register("email")} />
      <Input id="profile-birth-date" label="Date of birth" icon={CalendarBlank} disabled={!editing} type="date" error={errors.birthDate?.message} {...register("birthDate")} />
      <AgeField><span>Age</span><strong>{age === null ? "Not informed" : `${age} years`}</strong></AgeField>
      <Actions>
        {!editing && <Button type="button" variant="outline" icon={PencilSimple} onClick={(): void => { mutation.reset(); setEditing(true); }}>Edit profile</Button>}
        {editing && (
          <>
            <Button type="button" variant="outline" icon={X} disabled={mutation.isPending} onClick={(): void => { resetProfile(); setEditing(false); }}>Cancel</Button>
            <Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Saving…" : "Save profile"}</Button>
          </>
        )}
      </Actions>
    </Form>
  );
}
