import { CaretDown, Gear, SignOut, Users } from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import type { AppSection, UserMenuProps } from "../../types/navigationTypes";
import { useProfileAvatar } from "../../../modules/auth/profile/hooks/useProfileAvatar";
import { Avatar, AvatarImage, Container, Email, Menu, MenuButton, Name, Profile, Trigger } from "./styles";

export function UserMenu({ user, onLogout, isLoggingOut, onNavigate }: UserMenuProps): ReactElement {
  const [open, setOpen] = useState<boolean>(false);
  const avatar = useProfileAvatar(user.name);
  const initials: string = user.name.trim().split(/\s+/).slice(0, 2).map((part: string) => part[0]?.toUpperCase() ?? "").join("");
  const navigate = (section: AppSection): void => { setOpen(false); onNavigate(section); };
  return <Container><Trigger type="button" aria-haspopup="menu" aria-expanded={open} onClick={():void=>setOpen((value:boolean)=>!value)}><Avatar>{initials || "N"}{avatar.hasImage && <AvatarImage src={avatar.avatarUrl} alt="" onLoad={avatar.markLoaded} onError={avatar.markMissing} />}</Avatar><span>{user.name.split(" ")[0]}</span><CaretDown size={14} /></Trigger>{open && <Menu role="menu"><Profile><Name>{user.name}</Name><Email>{user.email} · {user.role}</Email></Profile><MenuButton type="button" role="menuitem" onClick={():void=>navigate("settings")}><Gear size={18} />Settings</MenuButton>{user.role === "OWNER" && <MenuButton type="button" role="menuitem" onClick={():void=>navigate("administration")}><Users size={18} />Administration</MenuButton>}<MenuButton type="button" role="menuitem" disabled={isLoggingOut} onClick={onLogout}><SignOut size={18} />{isLoggingOut ? "Leaving…" : "Log out"}</MenuButton></Menu>}</Container>;
}
