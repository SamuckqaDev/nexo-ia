import { LockKey } from "@phosphor-icons/react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { useEffect, type ReactElement } from "react";
import { Button } from "../../../components/Button";
import { Input } from "../../../components/Input";
import { useLoginForm } from "../../../../modules/auth/session/hooks/useLoginForm";
import { useSessionExpiredStore } from "../../../auth/sessionExpiredStore";
import { Actions, Backdrop, Dialog, Form, Icon, Message, Title } from "./styles";

export function SessionExpiredModal(): ReactElement | null {
  const isOpen: boolean = useSessionExpiredStore((state) => state.isOpen);
  const close: () => void = useSessionExpiredStore((state) => state.close);
  const { form, mutation, submit } = useLoginForm();
  const { register, formState: { errors } } = form;

  useEffect((): void => {
    if (mutation.isSuccess) close();
  }, [close, mutation.isSuccess]);

  if (!isOpen) return null;

  return (
    <DialogPrimitive.Root open modal onOpenChange={(open: boolean): void => { if (!open && !mutation.isPending) close(); }}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay asChild><Backdrop /></DialogPrimitive.Overlay>
        <DialogPrimitive.Content asChild>
          <Dialog aria-describedby="session-expired-message">
            <Icon><LockKey size={25} weight="duotone" /></Icon>
            <DialogPrimitive.Title asChild><Title>Session expired</Title></DialogPrimitive.Title>
            <DialogPrimitive.Description asChild><Message id="session-expired-message">Your session ended for security. Revalidate your account to continue without losing your workspace.</Message></DialogPrimitive.Description>
            <Form onSubmit={submit} noValidate>
              <Input id="session-identifier" label="Username or email" autoComplete="username" error={errors.identifier?.message} {...register("identifier")} />
              <Input id="session-password" label="Password" type="password" autoComplete="current-password" icon={LockKey} error={errors.password?.message} {...register("password")} />
              <Actions><Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Revalidating…" : "Revalidate session"}</Button></Actions>
            </Form>
          </Dialog>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
