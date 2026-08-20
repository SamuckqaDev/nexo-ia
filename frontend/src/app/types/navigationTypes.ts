import type { Icon } from "@phosphor-icons/react";
import type { AuthenticatedUser } from "../../modules/auth/types/authTypes";

export type AppSection = "home" | "chat" | "projects" | "cowork" | "tasks" | "vaults" | "skills" | "settings" | "administration";
export type NavigationItem = { id: AppSection; label: string; icon: Icon; ownerOnly?: boolean };
export type PlannedCapability = { title: string; description: string };
export type AppShellProps = {
  user: AuthenticatedUser;
  onLogout: () => void;
  isLoggingOut: boolean;
};
export type PlaceholderPageProps = {
  title: string;
  eyebrow: string;
  description: string;
  release: string;
  icon: Icon;
  capabilities: PlannedCapability[];
  onStartInChat: () => void;
};
export type SidebarAccountProps = AppShellProps & { onNavigate: (section: AppSection) => void; collapsed: boolean };
