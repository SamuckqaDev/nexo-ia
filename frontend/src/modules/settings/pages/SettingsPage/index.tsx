import { Brain, ChartDonut, Cpu, IdentificationCard, SlidersHorizontal, ShieldCheck } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { useThemeStore } from "../../../../app/stores/useThemeStore";
import type { ThemeMode, ThemeState } from "../../../../app/types/themeTypes";
import { ChangePasswordForm } from "../../../auth/credential/components/ChangePasswordForm";
import { ProfileAvatar } from "../../../auth/profile/components/ProfileAvatar";
import { ProfileForm } from "../../../auth/profile/components/ProfileForm";
import { ProviderStatusCard } from "../../../provider/components/ProviderStatusCard";
import { ProviderRegistry } from "../../../provider/components/ProviderRegistry";
import { UsageOverview } from "../../../usage/components/UsageOverview";
import { SessionList } from "../../../auth/session/components/SessionList";
import type { SettingsPageProps, SettingsSection } from "../../types/settingsTypes";
import { usePreferenceStore } from "../../stores/usePreferenceStore";
import type { AppLanguage, PreferenceState } from "../../types/preferenceTypes";
import {
  DataGrid,
  DataItem,
  Description,
  Content,
  Layout,
  NavButton,
  Navigation,
  Page,
  PageHeader,
  PreferenceField,
  PreferenceHeader,
  PreferenceGrid,
  PreferenceOption,
  ProfileContent,
  ProfileDetails,
  Section,
  SectionHeader,
  SectionIcon,
  Select,
  Status,
  Toggle,
  ToggleThumb,
  Title,
} from "./styles";

const settingsNavigation: Array<{ id: SettingsSection; label: string }> = [
  { id: "profile", label: "Profile" },
  { id: "security", label: "Security" },
  { id: "preferences", label: "Preferences" },
  { id: "providers", label: "Providers" },
  { id: "usage", label: "Usage and tokens" }
];

export function SettingsPage({ user, section, onSectionChange }: SettingsPageProps): ReactElement {
  const mode: ThemeMode = useThemeStore((state: ThemeState) => state.mode);
  const setMode: ThemeState["setMode"] = useThemeStore((state: ThemeState) => state.setMode);
  const language: AppLanguage = usePreferenceStore((state: PreferenceState) => state.language);
  const setLanguage: PreferenceState["setLanguage"] = usePreferenceStore((state: PreferenceState) => state.setLanguage);
  const thinkingEnabled: boolean = usePreferenceStore((state: PreferenceState) => state.thinkingEnabled);
  const setThinkingEnabled: PreferenceState["setThinkingEnabled"] = usePreferenceStore(
    (state: PreferenceState) => state.setThinkingEnabled
  );
  const createdAt: string = new Intl.DateTimeFormat("en", { dateStyle: "long" })
    .format(new Date(user.createdAt));

  return (
    <Page>
      <PageHeader>
        <span>Workspace configuration</span>
        <h1>Settings</h1>
        <Description>Manage your identity, security, model providers, and AI usage in one place.</Description>
      </PageHeader>

      <Layout>
        <Navigation aria-label="Settings sections">
          {settingsNavigation.map((item: { id: SettingsSection; label: string }) => (
            <NavButton
              key={item.id}
              type="button"
              $active={section === item.id}
              onClick={(): void => onSectionChange(item.id)}
            >
              {item.label}
            </NavButton>
          ))}
        </Navigation>

        <Content>

      {section === "profile" && (
        <Section>
          <SectionHeader>
            <SectionIcon><IdentificationCard size={22} weight="duotone" /></SectionIcon>
            <div><Title>Profile</Title><Description>Review your account details and change your profile photo.</Description></div>
          </SectionHeader>
          <ProfileContent>
            <ProfileAvatar name={user.name} />
            <ProfileDetails>
              <ProfileForm user={user} />
              <DataGrid>
                <DataItem><span>Role</span><strong>{user.role}</strong></DataItem>
                <DataItem><span>Member since</span><strong>{createdAt}</strong></DataItem>
              </DataGrid>
            </ProfileDetails>
          </ProfileContent>
        </Section>
      )}

      {section === "security" && (
        <Section>
          <SectionHeader>
            <SectionIcon><ShieldCheck size={22} weight="duotone" /></SectionIcon>
            <div><Title>Security and sessions</Title><Description>Protect your credentials and inspect connected devices.</Description></div>
          </SectionHeader>
          <ChangePasswordForm />
          <SessionList />
        </Section>
      )}

      {section === "preferences" && (
        <Section>
          <SectionHeader>
            <SectionIcon><SlidersHorizontal size={22} weight="duotone" /></SectionIcon>
            <div><Title>Preferences</Title><Description>Choose how Nexo looks, responds, and uses model resources.</Description></div>
          </SectionHeader>
          <PreferenceGrid>
            <PreferenceField>
              <strong>Language</strong>
              <span>This preference will drive the interface translation as localized content is added.</span>
              <Select value={language} onChange={(event): void => setLanguage(event.target.value as AppLanguage)}>
                <option value="en">English</option>
                <option value="pt-BR">Português (Brasil)</option>
              </Select>
            </PreferenceField>
            <PreferenceField>
              <strong>Theme</strong>
              <span>The visual theme is applied immediately across your workspace.</span>
              <Select value={mode} onChange={(event): void => setMode(event.target.value as ThemeMode)}>
                <option value="dark">Dark</option>
                <option value="light">Light</option>
              </Select>
            </PreferenceField>
            <PreferenceOption>
              <PreferenceHeader>
                <span aria-hidden="true"><Brain size={19} weight="duotone" /></span>
                <div>
                  <strong>Model Thinking</strong>
                  <small>{thinkingEnabled ? "On for new requests" : "Off for new requests"}</small>
                </div>
                <Toggle
                  type="button"
                  role="switch"
                  aria-checked={thinkingEnabled}
                  aria-label="Enable model Thinking"
                  $checked={thinkingEnabled}
                  onClick={(): void => setThinkingEnabled(!thinkingEnabled)}
                >
                  <ToggleThumb $checked={thinkingEnabled} />
                </Toggle>
              </PreferenceHeader>
              <p>
                Off asks supported models not to generate reasoning and Nexo always excludes it from saved
                conversation context. On streams real provider reasoning temporarily; it is never saved or
                sent back in later turns.
              </p>
              <small>Some models, including GPT-OSS, may still reason internally even when the trace is disabled.</small>
            </PreferenceOption>
          </PreferenceGrid>
        </Section>
      )}

      {section === "providers" && (
        <Section>
          <SectionHeader>
            <SectionIcon><Cpu size={22} weight="duotone" /></SectionIcon>
            <div><Title>Providers</Title><Description>Configure local or remote providers and privacy boundaries.</Description></div>
          </SectionHeader>
          <ProviderRegistry />
          <ProviderStatusCard />
        </Section>
      )}

      {section === "usage" && (
        <Section>
          <SectionHeader>
            <SectionIcon><ChartDonut size={22} weight="duotone" /></SectionIcon>
            <div><Title>Usage and tokens</Title><Description>Inspect your model consumption, latency, and processing location.</Description></div>
          </SectionHeader>
          <UsageOverview />
        </Section>
      )}
        </Content>
      </Layout>
    </Page>
  );
}
