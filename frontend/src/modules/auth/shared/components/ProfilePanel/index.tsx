import type { ReactElement } from "react";
import { SignOut } from "@phosphor-icons/react";
import { Button } from "../../../../../shared/components/Button";
import type { ProfilePanelProps } from "../../../types/authTypes";
import { Copy, Label, Meta, Name, Panel } from "./styles";

export function ProfilePanel({ user, onLogout, isLoggingOut }: ProfilePanelProps): ReactElement {
  return (
    <Panel>
      <Copy>
        <Label>Connected as</Label>
        <Name>{user.name}</Name>
        <Meta>@{user.username} · {user.email} · {user.role}</Meta>
      </Copy>
      <Button
        type="button"
        variant="outline"
        icon={SignOut}
        onClick={onLogout}
        disabled={isLoggingOut}
      >
        {isLoggingOut ? "Leaving…" : "Log out"}
      </Button>
    </Panel>
  );
}
