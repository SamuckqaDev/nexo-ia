import { SignOut } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type { ActiveSession } from "../../../types/sessionTypes";
import type { MemberSessionsProps } from "../../../types/userManagementTypes";
import { useMemberSessions } from "../../hooks/useMemberSessions";
import { Detail, Empty, Session, Sessions } from "./styles";

export function MemberSessions({ userId }: MemberSessionsProps): ReactElement {
  const { query, revoke, isRevoking } = useMemberSessions(userId);
  const sessions: ActiveSession[] = query.data ?? [];
  return <Sessions>
    {query.isLoading && <Empty>Loading sessions…</Empty>}
    {query.isSuccess && sessions.length === 0 && <Empty>No active sessions.</Empty>}
    {sessions.map((session: ActiveSession) => <Session key={session.id}>
      <Detail title={session.userAgent}>{session.lastIp} · {session.userAgent}</Detail>
      <Button type="button" variant="outline" icon={SignOut} disabled={isRevoking}
        onClick={(): void => revoke(session.id)}>Revoke</Button>
    </Session>)}
  </Sessions>;
}
