import { ChartDonut, Cpu, IdentificationCard, ShieldCheck } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { ChangePasswordForm } from "../../../auth/credential/components/ChangePasswordForm";
import { ProfileAvatar } from "../../../auth/profile/components/ProfileAvatar";
import { SessionList } from "../../../auth/session/components/SessionList";
import type { SettingsPageProps } from "../../types/settingsTypes";
import {
  DataGrid,
  DataItem,
  Description,
  Heading,
  Intro,
  PendingState,
  ProfileContent,
  Section,
  SectionHeader,
  SectionIcon,
  Stack,
  Status,
  Title
} from "./styles";

export function SettingsPage({ user }: SettingsPageProps): ReactElement {
  const createdAt: string = new Intl.DateTimeFormat("en", { dateStyle: "long" })
    .format(new Date(user.createdAt));

  return (
    <Stack>
      <Intro>
        <Heading>Settings</Heading>
        <Description>Manage your identity, security, model providers, and AI usage in one place.</Description>
      </Intro>

      <Section>
        <SectionHeader>
          <SectionIcon><IdentificationCard size={22} weight="duotone" /></SectionIcon>
          <div>
            <Title>Profile</Title>
            <Description>Review your account details and change your profile photo.</Description>
          </div>
        </SectionHeader>
        <ProfileContent>
          <ProfileAvatar name={user.name} />
          <DataGrid>
            <DataItem><span>Name</span><strong>{user.name}</strong></DataItem>
            <DataItem><span>Username</span><strong>@{user.username}</strong></DataItem>
            <DataItem><span>Email</span><strong>{user.email}</strong></DataItem>
            <DataItem><span>Role</span><strong>{user.role}</strong></DataItem>
            <DataItem><span>Member since</span><strong>{createdAt}</strong></DataItem>
          </DataGrid>
        </ProfileContent>
      </Section>

      <Section>
        <SectionHeader>
          <SectionIcon><ShieldCheck size={22} weight="duotone" /></SectionIcon>
          <div>
            <Title>Security and sessions</Title>
            <Description>Protect your credentials and inspect every device connected to your account.</Description>
          </div>
        </SectionHeader>
        <ChangePasswordForm />
        <SessionList />
      </Section>

      <Section>
        <SectionHeader>
          <SectionIcon><Cpu size={22} weight="duotone" /></SectionIcon>
          <div>
            <Title>Providers</Title>
            <Description>Configure local or remote model providers and their privacy boundaries.</Description>
          </div>
          <Status>Planned</Status>
        </SectionHeader>
        <PendingState>No provider configuration API is available in this increment.</PendingState>
      </Section>

      <Section>
        <SectionHeader>
          <SectionIcon><ChartDonut size={22} weight="duotone" /></SectionIcon>
          <div>
            <Title>Token usage</Title>
            <Description>Monitor prompt, response, embedding, image, provider, and budget consumption.</Description>
          </div>
          <Status>Planned</Status>
        </SectionHeader>
        <PendingState>Usage will appear after provider execution and accounting are implemented.</PendingState>
      </Section>
    </Stack>
  );
}
