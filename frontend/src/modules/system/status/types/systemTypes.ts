import type { z } from "zod";
import type { UseQueryResult } from "@tanstack/react-query";
import type { AuthenticatedUser } from "../../../auth/types/authTypes";
import type { systemResponseSchema } from "../schemas/systemResponseSchema";
import type { AppSection } from "../../../../app/types/navigationTypes";
import type { SettingsSection } from "../../../settings/types/settingsTypes";

export type SystemResponse = z.infer<typeof systemResponseSchema>;
export type SystemStatusResult = UseQueryResult<SystemResponse, Error>;

export type HomePageProps = {
  user: AuthenticatedUser;
  onNavigate: (section: AppSection) => void;
  onOpenSettings: (section: SettingsSection) => void;
};
