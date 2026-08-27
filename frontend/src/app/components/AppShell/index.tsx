import { CaretLeft, CaretRight, List, X } from "@phosphor-icons/react";
import { lazy, Suspense, useEffect, useState, type ReactElement } from "react";
import { Navigate, Route, Routes, useLocation, useNavigate, type NavigateFunction } from "react-router-dom";
import { cancelAllChatStreams, resetChatStreams } from "../../../modules/conversation/chat/hooks/useChatStream";
import { useImageGenerationStore } from "../../../modules/conversation/media/stores/useImageGenerationStore";
import type { ImageGenerationState } from "../../../modules/conversation/media/types/imageGenerationTypes";
import { useVaultCatalogStore } from "../../../modules/knowledge/vault/stores/useVaultCatalogStore";
import type { VaultCatalogState } from "../../../modules/knowledge/vault/types/vaultTypes";
import { useServerWorkspaceSelectionStore } from "../../../modules/project/workspace/stores/useServerWorkspaceSelectionStore";
import type { ServerWorkspaceSelectionState } from "../../../modules/project/workspace/stores/useServerWorkspaceSelectionStore";
import type { SettingsSection } from "../../../modules/settings/types/settingsTypes";
import { useSkillCatalogStore } from "../../../modules/skill/catalog/stores/useSkillCatalogStore";
import type { SkillCatalogState } from "../../../modules/skill/catalog/types/skillTypes";
import { Loading } from "../../../shared/components/Loading";
import type { AppSection, AppShellProps } from "../../types/navigationTypes";
import { SidebarAccount } from "../SidebarAccount";
import { AppBrand } from "./components/AppBrand";
import { SidebarNavigation } from "./components/SidebarNavigation";
import {
  EdgeToggle,
  Main,
  MobileMenuButton,
  MobileScrim,
  Shell,
  Sidebar,
  Workspace
} from "./styles";

const ChatPage = lazy(() => import("../../../modules/conversation/chat/pages/ChatPage")
  .then((module) => ({ default: module.ChatPage })));
const HomePage = lazy(() => import("../../../modules/system/status/pages/HomePage")
  .then((module) => ({ default: module.HomePage })));
const SettingsPage = lazy(() => import("../../../modules/settings/pages/SettingsPage")
  .then((module) => ({ default: module.SettingsPage })));
const UserManagement = lazy(() => import("../../../modules/auth/user/components/UserManagement")
  .then((module) => ({ default: module.UserManagement })));
const WorkspaceSwitcher = lazy(() => import("../../../modules/project/workspace/components/WorkspaceSwitcher")
  .then((module) => ({ default: module.WorkspaceSwitcher })));
const ProjectsPage = lazy(() => import("../../../modules/project/workspace/pages/ProjectsPage")
  .then((module) => ({ default: module.ProjectsPage })));
const CoworkPage = lazy(() => import("../../../modules/cowork/session/pages/CoworkPage")
  .then((module) => ({ default: module.CoworkPage })));
const CalendarPage = lazy(() => import("../../../modules/automation/calendar/pages/CalendarPage")
  .then((module) => ({ default: module.CalendarPage })));
const TeamsPage = lazy(() => import("../../../modules/organization/team/pages/TeamsPage")
  .then((module) => ({ default: module.TeamsPage })));
const VaultsPage = lazy(() => import("../../../modules/knowledge/vault/pages/VaultsPage")
  .then((module) => ({ default: module.VaultsPage })));
const SkillsPage = lazy(() => import("../../../modules/skill/catalog/pages/SkillsPage")
  .then((module) => ({ default: module.SkillsPage })));
const McpHubPage = lazy(() => import("../../../modules/mcp/catalog/pages/McpHubPage")
  .then((module) => ({ default: module.McpHubPage })));

const sectionPaths: Record<AppSection, string> = {
  home: "/",
  chat: "/chat",
  projects: "/projects",
  cowork: "/cowork",
  tasks: "/tasks",
  teams: "/teams",
  vaults: "/vaults",
  skills: "/skills",
  mcp: "/mcp",
  settings: "/settings/profile",
  administration: "/administration"
};

const sectionFromPath = (pathname: string): AppSection => {
  if (pathname.startsWith("/settings")) return "settings";
  if (pathname.startsWith("/administration")) return "administration";
  return (Object.entries(sectionPaths).find(([, path]: [string, string]) => path !== "/" && pathname.startsWith(path))?.[0] as AppSection | undefined) ?? "home";
};

const settingsFromPath = (pathname: string): SettingsSection => {
  const candidate: string | undefined = pathname.split("/")[2];
  return candidate === "security" || candidate === "preferences" || candidate === "providers" || candidate === "usage" ? candidate : "profile";
};

export function AppShell({ user, onLogout, isLoggingOut }: AppShellProps): ReactElement {
  const [collapsed, setCollapsed] = useState<boolean>(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);
  const [isPreparingLogout, setIsPreparingLogout] = useState<boolean>(false);
  const location = useLocation();
  const routerNavigate: NavigateFunction = useNavigate();
  const section: AppSection = sectionFromPath(location.pathname);
  const settingsSection: SettingsSection = settingsFromPath(location.pathname);
  const sidebarCollapsed: boolean = collapsed && !mobileMenuOpen;
  const initializeWorkspaceSelection: ServerWorkspaceSelectionState["initialize"] =
    useServerWorkspaceSelectionStore((state: ServerWorkspaceSelectionState) => state.initialize);
  const initializeVaults: VaultCatalogState["initialize"] = useVaultCatalogStore((state: VaultCatalogState) => state.initialize);
  const resetVaults: VaultCatalogState["reset"] = useVaultCatalogStore((state: VaultCatalogState) => state.reset);
  const initializeSkills: SkillCatalogState["initialize"] = useSkillCatalogStore((state: SkillCatalogState) => state.initialize);
  const resetSkills: SkillCatalogState["reset"] = useSkillCatalogStore((state: SkillCatalogState) => state.reset);
  const resetImageGeneration: ImageGenerationState["reset"] = useImageGenerationStore(
    (state: ImageGenerationState) => state.reset);

  useEffect((): (() => void) => {
    initializeWorkspaceSelection(user.id);
    initializeVaults(user.id);
    initializeSkills(user.id);
    return (): void => {
      resetChatStreams();
      resetImageGeneration();
      resetVaults();
      resetSkills();
    };
  }, [initializeSkills, initializeVaults, initializeWorkspaceSelection, resetImageGeneration, resetSkills, resetVaults, user.id]);

  const navigate = (targetSection: AppSection): void => {
    routerNavigate(sectionPaths[targetSection]);
    setMobileMenuOpen(false);
  };
  const openSettings = (targetSection: SettingsSection): void => {
    routerNavigate(`/settings/${targetSection}`);
    setMobileMenuOpen(false);
  };
  const endSession = (): void => {
    if (isPreparingLogout || isLoggingOut) return;
    setIsPreparingLogout(true);
    cancelAllChatStreams().finally((): void => {
      onLogout();
      setIsPreparingLogout(false);
    });
  };
  return (
    <Shell $collapsed={collapsed}>
      <Sidebar $collapsed={sidebarCollapsed} $mobileOpen={mobileMenuOpen}>
        <EdgeToggle
          type="button"
          title={collapsed ? "Expand menu" : "Collapse menu"}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          onClick={(): void => setCollapsed((value: boolean) => !value)}
        >
          {collapsed ? <CaretRight size={13} weight="bold" /> : <CaretLeft size={13} weight="bold" />}
        </EdgeToggle>

        <AppBrand collapsed={sidebarCollapsed} />

        <Suspense fallback={null}>
          <WorkspaceSwitcher collapsed={sidebarCollapsed} onManage={(): void => navigate("projects")} />
        </Suspense>

        <SidebarNavigation section={section} collapsed={sidebarCollapsed} onNavigate={navigate} />

        <SidebarAccount
          user={user}
          collapsed={sidebarCollapsed}
          onLogout={endSession}
          isLoggingOut={isLoggingOut || isPreparingLogout}
          onNavigate={navigate}
        />
      </Sidebar>

      {mobileMenuOpen && (
        <MobileScrim
          type="button"
          aria-label="Close navigation menu"
          onClick={(): void => setMobileMenuOpen(false)}
        />
      )}

      <Workspace>
        <MobileMenuButton
          type="button"
          $open={mobileMenuOpen}
          aria-label={mobileMenuOpen ? "Close navigation menu" : "Open navigation menu"}
          aria-expanded={mobileMenuOpen}
          onClick={(): void => setMobileMenuOpen((value: boolean) => !value)}
        >
          {mobileMenuOpen ? <X size={20} /> : <List size={20} />}
        </MobileMenuButton>
        <Main>
          <Suspense fallback={<Loading label="Opening workspace…" />}>
            <Routes>
              <Route path="/" element={<HomePage user={user} onNavigate={navigate} onOpenSettings={openSettings} />} />
              <Route path="/chat" element={<ChatPage />} />
              <Route path="/projects" element={<ProjectsPage onOpenChat={(): void => { routerNavigate("/chat?new=1"); }} />} />
              <Route path="/cowork" element={<CoworkPage />} />
              <Route path="/tasks" element={<CalendarPage />} />
              <Route path="/teams" element={<TeamsPage user={user} />} />
              <Route path="/vaults" element={<VaultsPage />} />
              <Route path="/skills" element={<SkillsPage />} />
              <Route path="/mcp" element={<McpHubPage />} />
              <Route path="/settings/:settingsSection" element={<SettingsPage user={user} section={settingsSection} onSectionChange={openSettings} />} />
              <Route path="/settings" element={<Navigate to="/settings/profile" replace />} />
              <Route path="/administration" element={<UserManagement />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </Main>
      </Workspace>
    </Shell>
  );
}
