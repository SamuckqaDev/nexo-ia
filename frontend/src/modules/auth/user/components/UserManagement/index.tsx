import { At, EnvelopeSimple, IdentificationCard, LockKey, UserMinus, UserPlus } from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import { PASSWORD_REQUIREMENTS } from "../../../../../shared/security/schemas/passwordSchema";
import type { ManagedUser } from "../../../types/userManagementTypes";
import { useUserManagement } from "../../hooks/useUserManagement";
import { MemberSessions } from "../MemberSessions";
import { Badge, Description, Form, Header, Identity, Item, List, Meta, Name, Panel, Title } from "./styles";

export function UserManagement(): ReactElement {
  const [sessionsUserId, setSessionsUserId] = useState<string | null>(null);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);
  const { users, form, isLoading, isCreating, updatingUserId, submit, changeStatus, generatePassword } = useUserManagement();
  const { register, formState: { errors } } = form;
  return <Panel aria-labelledby="users-title">
    <Header>
      <Title id="users-title">Team access</Title>
      <Description>Create Members and control access to this Nexo installation.</Description>
    </Header>
    <Form onSubmit={submit} noValidate>
      <Input label="Name" icon={IdentificationCard} error={errors.name?.message} {...register("name")} />
      <Input label="Username" icon={At} error={errors.username?.message} {...register("username")} />
      <Input label="Email" type="email" icon={EnvelopeSimple} error={errors.email?.message} {...register("email")} />
      <Input label="Temporary password" type="password" icon={LockKey} action={{ label: "Generate", onClick: generatePassword }} helperText={PASSWORD_REQUIREMENTS} error={errors.password?.message} {...register("password")} />
      <Button type="submit" icon={UserPlus} disabled={isCreating}>{isCreating ? "Creating…" : "Create Member"}</Button>
    </Form>
    {isLoading && <Description>Loading users…</Description>}
    <List>{users.map((user: ManagedUser) => <Item key={user.id}><Identity><Name>{user.name}<Badge $active={user.status === "ACTIVE"}>{user.status}</Badge></Name><Meta>@{user.username} · {user.email} · {user.role}</Meta></Identity>{user.role === "MEMBER" && <><Button type="button" variant="outline" onClick={():void=>setSessionsUserId(sessionsUserId === user.id ? null : user.id)}>{sessionsUserId === user.id ? "Hide sessions" : "Sessions"}</Button><Button type="button" variant="outline" icon={user.status === "ACTIVE" ? UserMinus : UserPlus} disabled={updatingUserId === user.id} onClick={():void=>{ if(user.status!=="ACTIVE"){changeStatus(user.id,"ACTIVE");return;} ask({title:"Disable this member?",message:`${user.name} will immediately lose access and every active session will be revoked.`,confirmLabel:"Disable member",tone:"danger"}).then((confirmed:boolean)=>{if(confirmed)changeStatus(user.id,"DISABLED");}); }}>{user.status === "ACTIVE" ? "Disable" : "Activate"}</Button>{sessionsUserId === user.id && <MemberSessions userId={user.id} />}</>}</Item>)}</List>
  </Panel>;
}
