import {
  Bell,
  Brain,
  CalendarCheck,
  ChatCircleDots,
  Gear,
  House,
  List,
  Moon,
  SidebarSimple,
  Sparkle,
  Sun,
  Users,
  Vault,
  X
} from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { UserManagement } from "../../../modules/auth/user/components/UserManagement";
import { SettingsPage } from "../../../modules/settings/pages/SettingsPage";
import { HomePage } from "../../../modules/system/status/pages/HomePage";
import { useThemeStore } from "../../stores/useThemeStore";
import type { AppSection, AppShellProps, NavigationItem } from "../../types/navigationTypes";
import type { ThemeState } from "../../types/themeTypes";
import { PlaceholderPage } from "../PlaceholderPage";
import { UserMenu } from "../UserMenu";
import {
  Brand,
  BrandName,
  Header,
  HeaderActions,
  HeaderIdentity,
  HeaderTitle,
  IconButton,
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

export function AppShell({ user, onLogout, isLoggingOut }: AppShellProps): ReactElement {
  const [section, setSection] = useState<AppSection>("home");
  const [collapsed, setCollapsed] = useState<boolean>(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);
  const mode: ThemeState["mode"] = useThemeStore((state: ThemeState) => state.mode);
  const toggleTheme: ThemeState["toggle"] = useThemeStore((state: ThemeState) => state.toggle);
  const currentItem: NavigationItem = [...featureNavigation, ...accountSections]
    .find((item: NavigationItem) => item.id === section) ?? featureNavigation[0];
  const navigate = (targetSection: AppSection): void => {
    setSection(targetSection);
    setMobileMenuOpen(false);
  };

  let content: ReactElement;

  if (section === "home") {
    content = <HomePage user={user} />;
  } else if (section === "settings") {
    content = <SettingsPage user={user} />;
  } else if (section === "administration") {
    content = <UserManagement />;
  } else {
    content = (
      <PlaceholderPage
        title={currentItem.label}
        description="This Nexo workspace is mapped and will be activated in its product increment."
        icon={currentItem.icon}
      />
    );
  }

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
            <IconButton
              type="button"
              aria-label={`Use ${mode === "dark" ? "light" : "dark"} theme`}
              onClick={toggleTheme}
            >
              {mode === "dark" ? <Sun size={20} /> : <Moon size={20} />}
            </IconButton>
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
        <Main>{content}</Main>
      </Workspace>
    </Shell>
  );
}
