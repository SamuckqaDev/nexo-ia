import type { ProjectWorkspace, WorkspaceAccess, WorkspaceCheck, WorkspacePlatform } from "./workspaceTypes";

export type WorkspaceRegistrationResult = {
  isSupported: boolean;
  isPicking: boolean;
  platform: WorkspacePlatform;
  actionLabel: string;
  error: string | null;
  chooseFolder: (access: WorkspaceAccess) => Promise<ProjectWorkspace | null>;
};

export type WorkspaceCheckResult = {
  checkActiveWorkspace: (requestPermission: boolean) => Promise<WorkspaceCheck>;
  acceptCurrentStructure: () => Promise<void>;
};

export type WorkspaceChangeNoticeProps = {
  check: WorkspaceCheck;
  workspaceName: string;
  onManage: () => void;
  onRecheck: () => void;
  onAccept: () => void;
};
