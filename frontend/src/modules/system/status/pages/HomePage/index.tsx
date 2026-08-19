import {
  ArrowRight,
  ChatCircleDots,
  Cpu,
  GraphicsCard,
  Plus,
  Robot,
  ShieldCheck,
  Sparkle,
  Vault
} from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { SystemStatus } from "../../components/SystemStatus";
import type { HomePageProps } from "../../types/systemTypes";
import {
  ActionButton,
  Actions,
  Columns,
  EmptyIcon,
  EmptyState,
  Eyebrow,
  Greeting,
  Hero,
  Intro,
  Metric,
  MetricRow,
  Page,
  Panel,
  PanelAction,
  PanelCopy,
  PanelHeader,
  PanelIcon,
  PanelTitle,
  ProviderDetails,
  ProviderState,
  Roadmap,
  RoadmapGrid,
  RoadmapHead,
  RoadmapItem,
  RoadmapTag,
  Stack,
  StatBody,
  StatIcon,
  StatStrip,
  StatTile,
  StatusDot,
  Summary,
  SystemCard
} from "./styles";

const firstName = (name: string): string => name.trim().split(/\s+/)[0] ?? name;

export function HomePage({ user, onNavigate, onOpenSettings }: HomePageProps): ReactElement {
  return (
    <Page>
      <Hero>
        <Intro>
          <Eyebrow>Local workspace</Eyebrow>
          <Greeting>Good to see you, {firstName(user.name)}.</Greeting>
          <Summary>
            Your assistant runs on your own machine. Start a conversation, connect a local model, and
            keep an eye on usage — all private to your account.
          </Summary>
          <Actions>
            <ActionButton type="button" onClick={(): void => onNavigate("chat")}>
              <Plus size={16} weight="bold" /> New chat
            </ActionButton>
            <ActionButton type="button" $secondary onClick={(): void => onOpenSettings("providers")}>
              <Cpu size={16} weight="duotone" /> Manage providers
            </ActionButton>
          </Actions>
        </Intro>
        <SystemCard>
          <ShieldCheck size={22} weight="duotone" />
          <div>
            <strong>Local foundation</strong>
            <SystemStatus />
          </div>
        </SystemCard>
      </Hero>

      <StatStrip>
        <StatTile type="button" onClick={(): void => onOpenSettings("providers")}>
          <StatIcon $accent><Cpu size={19} weight="duotone" /></StatIcon>
          <StatBody><span>Provider</span><strong>Not configured</strong></StatBody>
          <ArrowRight size={15} />
        </StatTile>
        <StatTile type="button" onClick={(): void => onNavigate("chat")}>
          <StatIcon><ChatCircleDots size={19} weight="duotone" /></StatIcon>
          <StatBody><span>Conversations</span><strong>None yet</strong></StatBody>
          <ArrowRight size={15} />
        </StatTile>
        <StatTile type="button" onClick={(): void => onOpenSettings("usage")}>
          <StatIcon><GraphicsCard size={19} weight="duotone" /></StatIcon>
          <StatBody><span>Tokens · 7 days</span><strong>—</strong></StatBody>
          <ArrowRight size={15} />
        </StatTile>
      </StatStrip>

      <Columns>
        <Stack>
          <Panel>
            <PanelHeader>
              <PanelCopy>
                <PanelIcon><ChatCircleDots size={19} weight="duotone" /></PanelIcon>
                <div><PanelTitle>Recent conversations</PanelTitle><span>Continue where you left off</span></div>
              </PanelCopy>
              <PanelAction type="button" onClick={(): void => onNavigate("chat")}>
                Open chat <ArrowRight size={14} />
              </PanelAction>
            </PanelHeader>
            <EmptyState>
              <EmptyIcon><ChatCircleDots size={22} weight="duotone" /></EmptyIcon>
              <strong>No conversations yet</strong>
              <span>Start your first chat and it will show up here, private to your account.</span>
            </EmptyState>
          </Panel>

          <Panel>
            <PanelHeader>
              <PanelCopy>
                <PanelIcon><GraphicsCard size={19} weight="duotone" /></PanelIcon>
                <div><PanelTitle>AI usage</PanelTitle><span>Your last 7 days</span></div>
              </PanelCopy>
              <PanelAction type="button" onClick={(): void => onOpenSettings("usage")}>
                View usage <ArrowRight size={14} />
              </PanelAction>
            </PanelHeader>
            <MetricRow>
              <Metric><span>Input</span><strong>—</strong></Metric>
              <Metric><span>Output</span><strong>—</strong></Metric>
              <Metric><span>Total</span><strong>—</strong></Metric>
            </MetricRow>
          </Panel>
        </Stack>

        <Stack>
          <Panel>
            <PanelHeader>
              <PanelCopy>
                <PanelIcon $accent><Cpu size={19} weight="duotone" /></PanelIcon>
                <div><PanelTitle>Current provider</PanelTitle><span>Model processing</span></div>
              </PanelCopy>
            </PanelHeader>
            <ProviderState><StatusDot $online={false} /> Not configured</ProviderState>
            <ProviderDetails>
              <span>Provider<strong>None selected</strong></span>
              <span>Model<strong>Unavailable</strong></span>
              <span>Processing<strong>Local</strong></span>
            </ProviderDetails>
            <PanelAction type="button" onClick={(): void => onOpenSettings("providers")}>
              Configure a provider <ArrowRight size={14} />
            </PanelAction>
          </Panel>
        </Stack>
      </Columns>

      <Roadmap>
        <RoadmapHead>
          <h2>Coming to Nexo IA</h2>
          <span>Not part of this release yet</span>
        </RoadmapHead>
        <RoadmapGrid>
          <RoadmapItem>
            <Vault size={18} weight="duotone" />
            <div>
              <strong>Knowledge Vaults</strong>
              <small>Ask questions over your own local documents with cited answers.</small>
              <RoadmapTag>Release 0.2</RoadmapTag>
            </div>
          </RoadmapItem>
          <RoadmapItem>
            <Sparkle size={18} weight="duotone" />
            <div>
              <strong>Workspaces</strong>
              <small>Open a project folder for Nexo to inspect and edit under permission.</small>
              <RoadmapTag>Release 0.3</RoadmapTag>
            </div>
          </RoadmapItem>
          <RoadmapItem>
            <Robot size={18} weight="duotone" />
            <div>
              <strong>Cowork &amp; automations</strong>
              <small>Collaborate on a visible plan and schedule governed, unattended runs.</small>
              <RoadmapTag>Release 0.6</RoadmapTag>
            </div>
          </RoadmapItem>
        </RoadmapGrid>
      </Roadmap>
    </Page>
  );
}
