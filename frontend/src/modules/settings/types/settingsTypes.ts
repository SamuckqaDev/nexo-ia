import type { AuthenticatedUser } from "../../auth/types/authTypes";

export type SettingsPageProps = {
  user: AuthenticatedUser;
  section: SettingsSection;
  onSectionChange: (section: SettingsSection) => void;
};

export type SettingsSection = "profile" | "security" | "preferences" | "providers" | "usage";
