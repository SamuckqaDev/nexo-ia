import type { Icon } from "@phosphor-icons/react";
import type { AuthenticatedUser } from "../../modules/auth/types/authTypes";

export type AppSection = "home" | "chat" | "cowork" | "tasks" | "vaults" | "skills" | "settings" | "administration";
export type NavigationItem = { id: AppSection; label: string; icon: Icon; ownerOnly?: boolean };
export type AppShellProps = {
  user: AuthenticatedUser;
  onLogout: () => void;
  isLoggingOut: boolean;
};
export type PlaceholderPageProps = { title: string; description: string; icon: Icon };
export type UserMenuProps = AppShellProps & { onNavigate: (section: AppSection) => void };
