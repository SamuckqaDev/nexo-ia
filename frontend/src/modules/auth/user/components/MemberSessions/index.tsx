import { SignOut } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import type { ActiveSession } from "../../../types/sessionTypes";
import type { MemberSessionsProps } from "../../../types/userManagementTypes";
import { useMemberSessions } from "../../hooks/useMemberSessions";
import { Detail, Empty, Session, Sessions } from "./styles";

export function MemberSessions({ userId }: MemberSessionsProps): ReactElement {
  const { query, revoke, isRevoking } = useMemberSessions(userId);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);
  const sessions: ActiveSession[] = query.data ?? [];
  return <Sessions>
    {query.isLoading && <Empty>Loading sessions…</Empty>}
    {query.isSuccess && sessions.length === 0 && <Empty>No active sessions.</Empty>}
    {sessions.map((session: ActiveSession) => <Session key={session.id}>
      <Detail title={session.userAgent}>{session.lastIp} · {session.userAgent}</Detail>
      <Button type="button" variant="outline" icon={SignOut} disabled={isRevoking}
        onClick={(): void => { ask({ title: "Revoke member session?", message: "The selected member device will lose access immediately.", confirmLabel: "Revoke session", tone: "danger" }).then((confirmed: boolean) => { if (confirmed) revoke(session.id); }); }}>Revoke</Button>
    </Session>)}
  </Sessions>;
}
