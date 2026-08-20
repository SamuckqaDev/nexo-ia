import type { WorkspacePlatform } from "../types/workspaceTypes";

type NavigatorWithPlatformData = Navigator & {
  userAgentData?: { platform?: string };
};

export function detectWorkspacePlatform(): WorkspacePlatform {
  const platformNavigator: NavigatorWithPlatformData = navigator;
  const platform: string = `${platformNavigator.userAgentData?.platform ?? navigator.platform} ${navigator.userAgent}`.toLowerCase();

  if (platform.includes("win")) return "windows";
  if (platform.includes("mac")) return "macos";
  if (platform.includes("linux") || platform.includes("x11")) return "linux";
  return "unknown";
}

export function workspacePlatformLabel(platform: WorkspacePlatform): string {
  if (platform === "windows") return "Windows";
  if (platform === "macos") return "macOS";
  if (platform === "linux") return "Linux";
  return "this device";
}

export function workspacePickerLabel(platform: WorkspacePlatform): string {
  if (platform === "windows") return "Choose with File Explorer";
  if (platform === "macos") return "Choose with Finder";
  if (platform === "linux") return "Choose with folder picker";
  return "Choose a folder on this device";
}
