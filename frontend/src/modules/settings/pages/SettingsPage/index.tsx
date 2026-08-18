import { ChartDonut, Cpu, IdentificationCard, SlidersHorizontal, ShieldCheck } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { useThemeStore } from "../../../../app/stores/useThemeStore";
import type { ThemeMode, ThemeState } from "../../../../app/types/themeTypes";
import { ChangePasswordForm } from "../../../auth/credential/components/ChangePasswordForm";
import { ProfileAvatar } from "../../../auth/profile/components/ProfileAvatar";
import { ProfileForm } from "../../../auth/profile/components/ProfileForm";
import { ProviderStatusCard } from "../../../provider/components/ProviderStatusCard";
import { ProviderRegistry } from "../../../provider/components/ProviderRegistry";
import { SessionList } from "../../../auth/session/components/SessionList";
import type { SettingsPageProps, SettingsSection } from "../../types/settingsTypes";
import { usePreferenceStore } from "../../stores/usePreferenceStore";
import type { AppLanguage, PreferenceState } from "../../types/preferenceTypes";
import {
  Chart,
  ChartEmpty,
  DataGrid,
  DataItem,
  Description,
  Metric,
  MetricGrid,
  NavButton,
  Navigation,
  Page,
  PendingState,
  PreferenceField,
  PreferenceGrid,
  PeriodButton,
  Periods,
  ProfileContent,
  ProfileDetails,
  Section,
  SectionHeader,
  SectionIcon,
  Select,
  Status,
  Title,
  UsageBreakdown,
  UsageItem
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
  const createdAt: string = new Intl.DateTimeFormat("en", { dateStyle: "long" })
    .format(new Date(user.createdAt));

  return (
    <Page>
      <Description>Manage your identity, security, model providers, and AI usage in one place.</Description>

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
            <div><Title>Preferences</Title><Description>Choose how Nexo looks and which language it should use.</Description></div>
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
            <div><Title>Usage and tokens</Title><Description>Inspect model consumption, latency, processing location, and estimated cost.</Description></div>
            <Status>Planned</Status>
          </SectionHeader>
          <Periods aria-label="Usage period">
            <PeriodButton type="button">7 days</PeriodButton>
            <PeriodButton type="button">30 days</PeriodButton>
            <PeriodButton type="button">This month</PeriodButton>
          </Periods>
          <MetricGrid>
            <Metric><span>Input tokens</span><strong>—</strong></Metric>
            <Metric><span>Output tokens</span><strong>—</strong></Metric>
            <Metric><span>Total tokens</span><strong>—</strong></Metric>
            <Metric><span>Requests</span><strong>—</strong></Metric>
            <Metric><span>Average latency</span><strong>—</strong></Metric>
            <Metric><span>Estimated cost</span><strong>—</strong></Metric>
          </MetricGrid>
          <Chart><ChartEmpty>No usage has been recorded yet</ChartEmpty></Chart>
          <UsageBreakdown>
            <UsageItem><span>By provider</span><strong>No data</strong></UsageItem>
            <UsageItem><span>By model</span><strong>No data</strong></UsageItem>
            <UsageItem><span>By capability</span><strong>Chat · Cowork · RAG · Images</strong></UsageItem>
            <UsageItem><span>Processing location</span><strong>Local or remote</strong></UsageItem>
          </UsageBreakdown>
          <PendingState>Usage details will populate after provider execution and accounting are implemented.</PendingState>
        </Section>
      )}
    </Page>
  );
}
