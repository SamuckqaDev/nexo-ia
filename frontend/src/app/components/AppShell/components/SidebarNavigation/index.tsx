import {
  Brain,
  CalendarCheck,
  ChatCircleDots,
  FolderOpen,
  House,
  PlugsConnected,
  Sparkle,
  UsersThree,
  Vault
} from "@phosphor-icons/react";
import { Fragment, type ReactElement } from "react";
import type {
  AppSection,
  NavigationItem,
  SidebarNavigationProps
} from "../../../../types/navigationTypes";
import { NavButton, NavDivider, NavLabel, Navigation, NavigationLabel } from "./styles";

const featureNavigation: NavigationItem[] = [
  { id: "home", label: "Home", icon: House },
  { id: "chat", label: "Chat", icon: ChatCircleDots },
  { id: "projects", label: "Projects", icon: FolderOpen },
  { id: "cowork", label: "Cowork", icon: Brain },
  { id: "tasks", label: "Tasks & calendar", icon: CalendarCheck },
  { id: "teams", label: "Teams", icon: UsersThree },
  { id: "vaults", label: "Vaults", icon: Vault },
  { id: "skills", label: "Skills", icon: Sparkle },
  { id: "mcp", label: "MCP Hub", icon: PlugsConnected }
];

const navGroupBreakAfter: AppSection = "tasks";

export function SidebarNavigation({
  section,
  collapsed,
  onNavigate
}: SidebarNavigationProps): ReactElement {
  return (
    <>
      <NavigationLabel $hidden={collapsed}>Workspace</NavigationLabel>
      <Navigation aria-label="Nexo features">
        {featureNavigation.map((item: NavigationItem) => (
          <Fragment key={item.id}>
            <NavButton
              type="button"
              title={collapsed ? item.label : undefined}
              $active={section === item.id}
              $collapsed={collapsed}
              onClick={(): void => onNavigate(item.id)}
            >
              <item.icon size={20} weight={section === item.id ? "fill" : "duotone"} />
              <NavLabel $hidden={collapsed}>{item.label}</NavLabel>
            </NavButton>
            {item.id === navGroupBreakAfter && <NavDivider $hidden={collapsed} />}
          </Fragment>
        ))}
      </Navigation>
    </>
  );
}
