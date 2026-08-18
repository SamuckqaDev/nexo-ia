import {
  Bell,
  Brain,
  CalendarCheck,
  ChatCircleDots,
  Gear,
  House,
  List,
  SidebarSimple,
  Sparkle,
  Users,
  Vault,
  X
} from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { Navigate, Route, Routes, useLocation, useNavigate, type NavigateFunction } from "react-router-dom";
import { UserManagement } from "../../../modules/auth/user/components/UserManagement";
import { SettingsPage } from "../../../modules/settings/pages/SettingsPage";
import type { SettingsSection } from "../../../modules/settings/types/settingsTypes";
import { HomePage } from "../../../modules/system/status/pages/HomePage";
import { ChatPage } from "../../../modules/conversation/chat/pages/ChatPage";
import type { AppSection, AppShellProps, NavigationItem } from "../../types/navigationTypes";
import { PlaceholderPage } from "../PlaceholderPage";
import { UserMenu } from "../UserMenu";
import {
  Brand,
  BrandName,
  Header,
  HeaderActions,
  HeaderIdentity,
  HeaderTitle,
  Logo,
  Main,
  MobileMenuButton,
  MobileNavigation,
  NavButton,
  NavLabel,
  Navigation,
  NavigationLabel,
  NotificationButton,
  Shell,
  Sidebar,
  SidebarToggle,
  Workspace
} from "./styles";

const featureNavigation: NavigationItem[] = [
  { id: "home", label: "Home", icon: House },
  { id: "chat", label: "Chat", icon: ChatCircleDots },
  { id: "cowork", label: "Cowork", icon: Brain },
  { id: "tasks", label: "Tasks & calendar", icon: CalendarCheck },
  { id: "vaults", label: "Vaults", icon: Vault },
  { id: "skills", label: "Skills", icon: Sparkle }
];

const accountSections: NavigationItem[] = [
  { id: "settings", label: "Settings", icon: Gear },
  { id: "administration", label: "Administration", icon: Users, ownerOnly: true }
];

const sectionPaths: Record<AppSection, string> = {
  home: "/",
  chat: "/chat",
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
  const currentItem: NavigationItem = [...featureNavigation, ...accountSections]
    .find((item: NavigationItem) => item.id === section) ?? featureNavigation[0];
  const navigate = (targetSection: AppSection): void => {
    routerNavigate(sectionPaths[targetSection]);
    setMobileMenuOpen(false);
  };
  const openSettings = (targetSection: SettingsSection): void => {
    routerNavigate(`/settings/${targetSection}`);
    setMobileMenuOpen(false);
  };
  const placeholder = (item: NavigationItem): ReactElement => <PlaceholderPage title={item.label} description="This Nexo workspace is mapped and will be activated in its product increment." icon={item.icon} />;

  return (
    <Shell $collapsed={collapsed}>
      <Sidebar $collapsed={collapsed}>
        <Brand>
          <Logo src="/assets/logo/nexo-ia-symbol.png" alt="" />
          <BrandName $hidden={collapsed}>Nexo IA</BrandName>
        </Brand>

        <NavigationLabel $hidden={collapsed}>Workspace</NavigationLabel>
        <Navigation aria-label="Nexo features">
          {featureNavigation.map((item: NavigationItem) => (
            <NavButton
              key={item.id}
              type="button"
              title={collapsed ? item.label : undefined}
              $active={section === item.id}
              $collapsed={collapsed}
              onClick={(): void => navigate(item.id)}
            >
              <item.icon size={20} weight={section === item.id ? "fill" : "duotone"} />
              <NavLabel $hidden={collapsed}>{item.label}</NavLabel>
            </NavButton>
          ))}
        </Navigation>

        <SidebarToggle
          type="button"
          title={collapsed ? "Expand menu" : undefined}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          $collapsed={collapsed}
          onClick={(): void => setCollapsed((value: boolean) => !value)}
        >
          <SidebarSimple size={20} weight="duotone" />
          <NavLabel $hidden={collapsed}>{collapsed ? "Expand menu" : "Collapse menu"}</NavLabel>
        </SidebarToggle>
      </Sidebar>

      <Workspace>
        <Header>
          <HeaderIdentity>
            <MobileMenuButton
              type="button"
              $collapsed={false}
              aria-label={mobileMenuOpen ? "Close navigation menu" : "Open navigation menu"}
              aria-expanded={mobileMenuOpen}
              onClick={(): void => setMobileMenuOpen((value: boolean) => !value)}
            >
              {mobileMenuOpen ? <X size={20} /> : <List size={20} />}
            </MobileMenuButton>
            <HeaderTitle>
              <strong>{currentItem.label}</strong>
              <span>Local intelligence. Governed action.</span>
            </HeaderTitle>
          </HeaderIdentity>
          <HeaderActions>
            <NotificationButton type="button" aria-label="Notifications">
              <Bell size={20} />
            </NotificationButton>
            <UserMenu
              user={user}
              onLogout={onLogout}
              isLoggingOut={isLoggingOut}
              onNavigate={navigate}
            />
          </HeaderActions>
          <MobileNavigation $open={mobileMenuOpen} aria-label="Nexo features">
            {featureNavigation.map((item: NavigationItem) => (
              <NavButton
                key={item.id}
                type="button"
                $active={section === item.id}
                $collapsed={false}
                onClick={(): void => navigate(item.id)}
              >
                <item.icon size={20} weight={section === item.id ? "fill" : "duotone"} />
                <NavLabel $hidden={false}>{item.label}</NavLabel>
              </NavButton>
            ))}
          </MobileNavigation>
        </Header>
        <Main $wide={section === "chat"}>
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
        </Main>
      </Workspace>
    </Shell>
  );
}
