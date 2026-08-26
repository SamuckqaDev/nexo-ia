import type { Icon } from "@phosphor-icons/react";
import type { AuthenticatedUser } from "../../modules/auth/types/authTypes";

export type AppSection = "home" | "chat" | "projects" | "cowork" | "tasks" | "teams" | "vaults" | "skills" | "mcp" | "settings" | "administration";
export type NavigationItem = { id: AppSection; label: string; icon: Icon; ownerOnly?: boolean };
export type AppShellProps = {
  user: AuthenticatedUser;
  onLogout: () => void;
  isLoggingOut: boolean;
};
export type AppBrandProps = { collapsed: boolean };
export type SidebarNavigationProps = {
  section: AppSection;
  collapsed: boolean;
  onNavigate: (section: AppSection) => void;
};
export type SidebarAccountProps = AppShellProps & { onNavigate: (section: AppSection) => void; collapsed: boolean };
