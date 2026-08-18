import {
  ArrowRight,
  BellRinging,
  Brain,
  CalendarCheck,
  ChartLineUp,
  ChatCircleDots,
  ClockCountdown,
  Cpu,
  Plus,
  ShieldCheck,
  WarningCircle
} from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { SystemStatus } from "../../components/SystemStatus";
import type { HomePageProps } from "../../types/systemTypes";
import {
  ActionButton,
  Actions,
  Chart,
  ChartBaseline,
  ChartEmpty,
  EmptyIcon,
  EmptyState,
  Eyebrow,
  Greeting,
  Hero,
  Intro,
  Metric,
  MetricGrid,
  Page,
  Panel,
  PanelAction,
  PanelCopy,
  PanelGrid,
  PanelHeader,
  PanelIcon,
  PanelTitle,
  ProviderDetails,
  ProviderState,
  RunLegend,
  RunLegendItem,
  StatusCard,
  StatusDot,
  Summary,
  WidePanel
} from "./styles";

const firstName = (name: string): string => name.trim().split(/\s+/)[0] ?? name;

export function HomePage({ user, onNavigate, onOpenSettings }: HomePageProps): ReactElement {
  return (
    <Page>
      <Hero>
        <Intro>
          <Eyebrow>Operational workspace</Eyebrow>
          <Greeting>Good to see you, {firstName(user.name)}.</Greeting>
          <Summary>
            Follow conversations, provider health, AI usage and governed work from one place.
          </Summary>
          <Actions>
            <ActionButton type="button" onClick={(): void => onNavigate("chat")}>
              <Plus size={18} weight="bold" /> New chat
            </ActionButton>
            <ActionButton type="button" $secondary onClick={(): void => onNavigate("cowork")}>
              <Brain size={18} weight="duotone" /> Start Cowork
            </ActionButton>
          </Actions>
        </Intro>
        <StatusCard>
          <ShieldCheck size={24} weight="duotone" />
          <div>
            <strong>Local foundation online</strong>
            <SystemStatus />
          </div>
        </StatusCard>
      </Hero>

      <PanelGrid>
        <WidePanel>
          <PanelHeader>
            <PanelCopy>
              <PanelIcon><ChartLineUp size={21} weight="duotone" /></PanelIcon>
              <div><PanelTitle>AI usage</PanelTitle><span>Tokens over the last 7 days</span></div>
            </PanelCopy>
            <PanelAction type="button" onClick={(): void => onOpenSettings("usage")}>
              View usage <ArrowRight size={15} />
            </PanelAction>
          </PanelHeader>
          <MetricGrid>
            <Metric><span>Input</span><strong>—</strong></Metric>
            <Metric><span>Output</span><strong>—</strong></Metric>
            <Metric><span>Total</span><strong>—</strong></Metric>
          </MetricGrid>
          <Chart aria-label="No token usage recorded">
            <ChartBaseline />
            <ChartEmpty>No usage recorded yet</ChartEmpty>
          </Chart>
        </WidePanel>

        <Panel>
          <PanelHeader>
            <PanelCopy>
              <PanelIcon $accent><Cpu size={21} weight="duotone" /></PanelIcon>
              <div><PanelTitle>Current provider</PanelTitle><span>Model processing status</span></div>
            </PanelCopy>
          </PanelHeader>
          <ProviderState><StatusDot $online={false} /> Not configured</ProviderState>
          <ProviderDetails>
            <span>Provider<strong>None selected</strong></span>
            <span>Model<strong>Unavailable</strong></span>
            <span>Processing<strong>Not started</strong></span>
          </ProviderDetails>
          <PanelAction type="button" onClick={(): void => onOpenSettings("providers")}>
            Manage providers <ArrowRight size={15} />
          </PanelAction>
        </Panel>

        <Panel>
          <PanelHeader>
            <PanelCopy>
              <PanelIcon><ChatCircleDots size={21} weight="duotone" /></PanelIcon>
              <div><PanelTitle>Recent conversations</PanelTitle><span>Continue where you left off</span></div>
            </PanelCopy>
          </PanelHeader>
          <EmptyState>
            <EmptyIcon><ChatCircleDots size={24} weight="duotone" /></EmptyIcon>
            <strong>No conversations yet</strong>
            <span>Your latest chats will appear here.</span>
          </EmptyState>
          <PanelAction type="button" onClick={(): void => onNavigate("chat")}>
            Start first chat <ArrowRight size={15} />
          </PanelAction>
        </Panel>

        <WidePanel>
          <PanelHeader>
            <PanelCopy>
              <PanelIcon $accent><ClockCountdown size={21} weight="duotone" /></PanelIcon>
              <div><PanelTitle>Runs and schedules</PanelTitle><span>Cowork, tasks and autonomous work</span></div>
            </PanelCopy>
            <PanelAction type="button" onClick={(): void => onNavigate("tasks")}>
              Open calendar <ArrowRight size={15} />
            </PanelAction>
          </PanelHeader>
          <RunLegend>
            <RunLegendItem><CalendarCheck size={17} /><span>Upcoming</span><strong>0</strong></RunLegendItem>
            <RunLegendItem><Brain size={17} /><span>Running</span><strong>0</strong></RunLegendItem>
            <RunLegendItem $error><WarningCircle size={17} /><span>Needs attention</span><strong>0</strong></RunLegendItem>
          </RunLegend>
          <EmptyState $compact>
            <strong>No scheduled runs</strong>
            <span>Upcoming Cowork and automation executions, including failures, will appear here.</span>
          </EmptyState>
        </WidePanel>

        <Panel>
          <PanelHeader>
            <PanelCopy>
              <PanelIcon><BellRinging size={21} weight="duotone" /></PanelIcon>
              <div><PanelTitle>Approvals</PanelTitle><span>Decisions waiting for you</span></div>
            </PanelCopy>
          </PanelHeader>
          <EmptyState>
            <EmptyIcon><ShieldCheck size={24} weight="duotone" /></EmptyIcon>
            <strong>Nothing waiting</strong>
            <span>Permission and execution approvals will appear here.</span>
          </EmptyState>
        </Panel>
      </PanelGrid>
    </Page>
  );
}
