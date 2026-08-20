import {
  Brain,
  CalendarCheck,
  CaretLeft,
  CaretRight,
  ChatCircleDots,
  House,
  FolderOpen,
  List,
  Sparkle,
  Vault,
  X
} from "@phosphor-icons/react";
import { lazy, Suspense, useState, type ReactElement } from "react";
import { Navigate, Route, Routes, useLocation, useNavigate, type NavigateFunction } from "react-router-dom";
import type { SettingsSection } from "../../../modules/settings/types/settingsTypes";
import { HomePage } from "../../../modules/system/status/pages/HomePage";
import { Loading } from "../../../shared/components/Loading";
import type { AppSection, AppShellProps, NavigationItem, PlannedCapability } from "../../types/navigationTypes";
import { PlaceholderPage } from "../PlaceholderPage";
import { SidebarAccount } from "../SidebarAccount";
import {
  Brand,
  BrandName,
  EdgeToggle,
  Logo,
  Main,
  MobileMenuButton,
  MobileScrim,
  NavButton,
  NavLabel,
  Navigation,
  NavigationLabel,
  Shell,
  Sidebar,
  Workspace
} from "./styles";

const ChatPage = lazy(() => import("../../../modules/conversation/chat/pages/ChatPage")
  .then((module) => ({ default: module.ChatPage })));
const SettingsPage = lazy(() => import("../../../modules/settings/pages/SettingsPage")
  .then((module) => ({ default: module.SettingsPage })));
const UserManagement = lazy(() => import("../../../modules/auth/user/components/UserManagement")
  .then((module) => ({ default: module.UserManagement })));

const featureNavigation: NavigationItem[] = [
  { id: "home", label: "Home", icon: House },
  { id: "chat", label: "Chat", icon: ChatCircleDots },
  { id: "projects", label: "Projects", icon: FolderOpen },
  { id: "cowork", label: "Cowork", icon: Brain },
  { id: "tasks", label: "Tasks & calendar", icon: CalendarCheck },
  { id: "vaults", label: "Vaults", icon: Vault },
  { id: "skills", label: "Skills", icon: Sparkle }
];

type PlannedSection = {
  eyebrow: string;
  description: string;
  release: string;
  capabilities: PlannedCapability[];
};

const plannedSections: Record<"projects" | "cowork" | "tasks" | "vaults" | "skills", PlannedSection> = {
  projects: {
    eyebrow: "Governed workspaces",
    description: "Authorize an exact project directory so Nexo can understand its structure, inspect files, propose changes, show diffs, run approved commands and verify the result.",
    release: "Release 0.3",
    capabilities: [
      { title: "Scoped access", description: "Canonical workspace roots and explicit read, edit and command grants." },
      { title: "Project context", description: "Instructions, files, Git state and relevant knowledge stay attached to the project." },
      { title: "Visible changes", description: "Review proposed edits and diffs before accepting consequential work." },
      { title: "Verification", description: "Builds, tests and observable evidence determine whether work is complete." }
    ]
  },
  cowork: {
    eyebrow: "Durable objectives",
    description: "Collaborate with Nexo on long-running work through a visible plan, human checkpoints, activity history and verifiable deliverables.",
    release: "Release 0.6",
    capabilities: [
      { title: "Objective and scope", description: "Define the outcome, constraints, context and completion criteria." },
      { title: "Visible plan", description: "Follow milestones, dependencies, progress and replanning decisions." },
      { title: "Human checkpoints", description: "Pause for feedback, permission or decisions without losing context." },
      { title: "Deliverables", description: "Collect artifacts, evidence and a final report in one durable session." }
    ]
  },
  tasks: {
    eyebrow: "Scheduled work",
    description: "See Cowork milestones, approval deadlines and pre-authorized automations in one calendar with run history and conflict awareness.",
    release: "Release 0.6",
    capabilities: [
      { title: "Calendar views", description: "Move between month, week, day and agenda views." },
      { title: "Automations", description: "Create one-time or recurring work with timezone and execution limits." },
      { title: "Run control", description: "Preview safely, run now, reschedule, pause or inspect history." },
      { title: "Decision queue", description: "Surface approvals, failures and human input that block progress." }
    ]
  },
  vaults: {
    eyebrow: "Grounded knowledge",
    description: "Organize portable local sources, inspect their relationships and ask questions with links back to the exact supporting passages.",
    release: "Release 0.2",
    capabilities: [
      { title: "Vault Explorer", description: "Browse files, graph, tags, relationships, activity and permissions." },
      { title: "Hybrid retrieval", description: "Combine text, embeddings, metadata and explicit knowledge links." },
      { title: "Citations", description: "Preserve source, section, version and relationship provenance." },
      { title: "Lifecycle", description: "Reindex, share, export, archive and delete under explicit grants." }
    ]
  },
  skills: {
    eyebrow: "Reusable capability",
    description: "Package instructions, references, assets and scripts into governed workflows that Nexo can select explicitly or when their scope matches the task.",
    release: "Release 0.4",
    capabilities: [
      { title: "Skill library", description: "Inspect built-in, organization, team, project and personal Skills." },
      { title: "Progressive loading", description: "Load instructions and resources only when a Skill is relevant." },
      { title: "Declared dependencies", description: "Review tools, Vaults, providers and permissions before execution." },
      { title: "Validation", description: "Test activation, contracts, safety boundaries and expected artifacts." }
    ]
  }
};

const sectionPaths: Record<AppSection, string> = {
  home: "/",
  chat: "/chat",
  projects: "/projects",
  cowork: "/cowork",
  tasks: "/tasks",
  vaults: "/vaults",
  skills: "/skills",
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
  const location = useLocation();
  const routerNavigate: NavigateFunction = useNavigate();
  const section: AppSection = sectionFromPath(location.pathname);
  const settingsSection: SettingsSection = settingsFromPath(location.pathname);
  const sidebarCollapsed: boolean = collapsed && !mobileMenuOpen;
  const navigate = (targetSection: AppSection): void => {
    routerNavigate(sectionPaths[targetSection]);
    setMobileMenuOpen(false);
  };
  const openSettings = (targetSection: SettingsSection): void => {
    routerNavigate(`/settings/${targetSection}`);
    setMobileMenuOpen(false);
  };
  const placeholder = (item: NavigationItem): ReactElement => {
    const planned: PlannedSection = plannedSections[item.id as keyof typeof plannedSections];
    return (
      <PlaceholderPage
        title={item.label}
        eyebrow={planned.eyebrow}
        description={planned.description}
        release={planned.release}
        icon={item.icon}
        capabilities={planned.capabilities}
        onStartInChat={(): void => navigate("chat")}
      />
    );
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

        <Brand>
          <Logo src="/assets/logo/nexo-ia-symbol.png" alt="" />
          <BrandName $hidden={sidebarCollapsed}>Nexo IA</BrandName>
        </Brand>

        <NavigationLabel $hidden={sidebarCollapsed}>Workspace</NavigationLabel>
        <Navigation aria-label="Nexo features">
          {featureNavigation.map((item: NavigationItem) => (
            <NavButton
              key={item.id}
              type="button"
              title={sidebarCollapsed ? item.label : undefined}
              $active={section === item.id}
              $collapsed={sidebarCollapsed}
              onClick={(): void => navigate(item.id)}
            >
              <item.icon size={20} weight={section === item.id ? "fill" : "duotone"} />
              <NavLabel $hidden={sidebarCollapsed}>{item.label}</NavLabel>
            </NavButton>
          ))}
        </Navigation>

        <SidebarAccount
          user={user}
          collapsed={sidebarCollapsed}
          onLogout={onLogout}
          isLoggingOut={isLoggingOut}
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
              {featureNavigation.filter((item: NavigationItem) => item.id !== "home" && item.id !== "chat").map((item: NavigationItem) => (
                <Route key={item.id} path={sectionPaths[item.id]} element={placeholder(item)} />
              ))}
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
