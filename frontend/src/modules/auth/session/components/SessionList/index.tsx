import { Desktop, DeviceMobile, SignOut } from "@phosphor-icons/react";
import type { Icon } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type { ActiveSession } from "../../../types/sessionTypes";
import { useSessionManagement } from "../../hooks/useSessionManagement";
import {
  Action,
  Count,
  CurrentBadge,
  Description,
  Details,
  DeviceIcon,
  DeviceName,
  Header,
  Heading,
  Item,
  List,
  Metadata,
  Panel,
  State
} from "./styles";

const dateFormatter: Intl.DateTimeFormat = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short"
});

function isMobile(userAgent: string): boolean {
  return /android|iphone|ipad|mobile/i.test(userAgent);
}

function deviceName(userAgent: string): string {
  if (/iphone|ipad/i.test(userAgent)) return "Apple mobile device";
  if (/android/i.test(userAgent)) return "Android device";
  if (/windows/i.test(userAgent)) return "Windows computer";
  if (/macintosh|mac os/i.test(userAgent)) return "macOS computer";
  if (/linux/i.test(userAgent)) return "Linux computer";
  return "Unknown device";
}

export function SessionList(): ReactElement {
  const { query, revokeMutation, revoke, revokeOthers, isRevokingOthers } = useSessionManagement();
  const sessions: ActiveSession[] = query.data ?? [];
  const hasOtherSessions: boolean = sessions.some((session: ActiveSession) => !session.current);

  return (
    <Panel aria-labelledby="sessions-title">
      <Header>
        <div>
          <Heading id="sessions-title">Sessions and devices</Heading>
          <Description>Review where your Nexo account is currently connected.</Description>
        </div>
        <Count>{sessions.length} active</Count>
      </Header>

      {hasOtherSessions && (
        <Button type="button" variant="outline" icon={SignOut}
          disabled={isRevokingOthers} onClick={revokeOthers}>
          {isRevokingOthers ? "Revoking…" : "Revoke all other sessions"}
        </Button>
      )}

      {query.isLoading && <State>Loading active sessions…</State>}
      {query.isError && <State role="alert">Unable to load active sessions.</State>}
      {query.isSuccess && sessions.length === 0 && <State>No active sessions found.</State>}
      {sessions.length > 0 && (
        <List>
          {sessions.map((session: ActiveSession): ReactElement => {
            const Device: Icon = isMobile(session.userAgent) ? DeviceMobile : Desktop;
            return (
              <Item key={session.id}>
                <DeviceIcon><Device aria-hidden size={22} weight="duotone" /></DeviceIcon>
                <Details>
                  <DeviceName>
                    {deviceName(session.userAgent)}
                    {session.current && <CurrentBadge>Current</CurrentBadge>}
                  </DeviceName>
                  <Metadata title={session.userAgent}>{session.userAgent}</Metadata>
                  <Metadata>
                    IP {session.lastIp} · Last activity {dateFormatter.format(new Date(session.lastSeenAt))}
                  </Metadata>
                </Details>
                {!session.current && (
                  <Action>
                    <Button
                      type="button"
                      variant="outline"
                      icon={SignOut}
                      disabled={revokeMutation.isPending}
                      onClick={(): void => revoke(session.id)}
                    >
                      Revoke
                    </Button>
                  </Action>
                )}
              </Item>
            );
          })}
        </List>
      )}
    </Panel>
  );
}
