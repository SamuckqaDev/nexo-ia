import { CaretUpDown, Gear, SignOut, Users } from "@phosphor-icons/react";
import { useEffect, useRef, useState, type ReactElement } from "react";
import { useProfileAvatar } from "../../../modules/auth/profile/hooks/useProfileAvatar";
import { useConfirmationStore } from "../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../shared/feedback/types/confirmationTypes";
import type { AppSection, SidebarAccountProps } from "../../types/navigationTypes";
import {
  Account,
  Avatar,
  AvatarImage,
  Email,
  Identity,
  Menu,
  MenuButton,
  Name,
  Trigger
} from "./styles";

/**
 * The logged-in person and their account actions, seated at the foot of the sidebar. The menu opens
 * upward so it stays clear of the workspace, and collapses to just the avatar with the sidebar.
 */
export function SidebarAccount({
  user,
  collapsed,
  onLogout,
  isLoggingOut,
  onNavigate
}: SidebarAccountProps): ReactElement {
  const [open, setOpen] = useState<boolean>(false);
  const container = useRef<HTMLDivElement>(null);
  const avatar = useProfileAvatar(user.name);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);
  const initials: string = user.name.trim().split(/\s+/).slice(0, 2)
    .map((part: string) => part[0]?.toUpperCase() ?? "").join("");

  useEffect((): (() => void) | void => {
    if (!open) return;
    const close = (event: MouseEvent): void => {
      if (!container.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return (): void => document.removeEventListener("mousedown", close);
  }, [open]);

  const navigate = (section: AppSection): void => {
    setOpen(false);
    onNavigate(section);
  };

  const confirmLogout = (): void => {
    setOpen(false);
    ask({
      title: "Log out of Nexo?",
      message: "Your current session will end and you will need to authenticate again.",
      confirmLabel: "Log out",
      tone: "warning"
    }).then((confirmed: boolean): void => { if (confirmed) onLogout(); });
  };

  return (
    <Account ref={container}>
      {open && (
        <Menu role="menu">
          <MenuButton type="button" role="menuitem" onClick={(): void => navigate("settings")}>
            <Gear size={18} /> Settings
          </MenuButton>
          {user.role === "OWNER" && (
            <MenuButton type="button" role="menuitem" onClick={(): void => navigate("administration")}>
              <Users size={18} /> Administration
            </MenuButton>
          )}
          <MenuButton type="button" role="menuitem" disabled={isLoggingOut} onClick={confirmLogout}>
            <SignOut size={18} /> {isLoggingOut ? "Leaving…" : "Log out"}
          </MenuButton>
        </Menu>
      )}
      <Trigger
        type="button"
        $collapsed={collapsed}
        aria-haspopup="menu"
        aria-expanded={open}
        title={collapsed ? user.name : undefined}
        onClick={(): void => setOpen((value: boolean) => !value)}
      >
        <Avatar>
          {initials || "N"}
          {avatar.hasImage && (
            <AvatarImage src={avatar.avatarUrl} alt="" onLoad={avatar.markLoaded} onError={avatar.markMissing} />
          )}
        </Avatar>
        {!collapsed && (
          <Identity>
            <Name>{user.name}</Name>
            <Email>{user.email} · {user.role}</Email>
          </Identity>
        )}
        {!collapsed && <CaretUpDown size={15} />}
      </Trigger>
    </Account>
  );
}
