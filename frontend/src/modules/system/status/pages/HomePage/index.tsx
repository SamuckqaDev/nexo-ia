import {
  ArrowRight,
  Brain,
  CalendarCheck,
  ChatCircleDots,
  Cpu,
  FolderOpen,
  GraphicsCard,
  Robot,
  ShieldCheck,
  Sparkle,
  Vault
} from "@phosphor-icons/react";
import { useState, type FormEvent, type ReactElement } from "react";
import { useConversations } from "../../../../conversation/chat/hooks/useChat";
import { useChatDraftStore } from "../../../../conversation/chat/stores/useChatDraftStore";
import type { ChatDraftState } from "../../../../conversation/chat/types/chatDraftTypes";
import type { Conversation } from "../../../../conversation/chat/types/chatTypes";
import { useProviderRegistry } from "../../../../provider/hooks/useProviderRegistry";
import type { ProviderConfiguration } from "../../../../provider/types/providerConfigurationTypes";
import { useUsage } from "../../../../usage/hooks/useUsage";
import { SystemStatus } from "../../components/SystemStatus";
import type { HomePageProps } from "../../types/systemTypes";
import {
  ActionButton,
  Actions,
  CapabilityGrid,
  CapabilityItem,
  CapabilityTag,
  Columns,
  CommandComposer,
  CommandFooter,
  CommandHint,
  CommandInput,
  CommandSubmit,
  ConversationList,
  ConversationRow,
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
  SectionHead,
  WorkspaceSection,
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
const formatNumber = (value: number | undefined): string =>
  value === undefined ? "—" : new Intl.NumberFormat().format(value);

export function HomePage({ user, onNavigate, onOpenSettings }: HomePageProps): ReactElement {
  const conversations = useConversations();
  const providers = useProviderRegistry().registry;
  const usage = useUsage("LAST_7_DAYS");
  const configuredProvider: ProviderConfiguration | undefined = providers.data
    ?.find((provider: ProviderConfiguration) => provider.enabled && Boolean(provider.selectedModel));
  const recentConversations: Conversation[] = conversations.data?.slice(0, 3) ?? [];
  const [request, setRequest] = useState<string>("");
  const setDraft: ChatDraftState["setContent"] = useChatDraftStore((state: ChatDraftState) => state.setContent);

  const startRequest = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    const content: string = request.trim();
    if (!content) return;
    setDraft(content);
    onNavigate("chat");
  };

  return (
    <Page>
      <Hero>
        <Intro>
          <Eyebrow>Governed AI workspace</Eyebrow>
          <Greeting>What do you want to accomplish, {firstName(user.name)}?</Greeting>
          <Summary>
            Ask a question now, or bring a project objective for Nexo to analyze, plan, implement,
            validate and report with visible permissions and evidence.
          </Summary>
          <CommandComposer onSubmit={startRequest}>
            <CommandInput
              aria-label="Describe what you want Nexo to do"
              placeholder="Ask a question, explain a problem, or describe what you want to build…"
              value={request}
              onChange={(event): void => setRequest(event.target.value)}
            />
            <CommandFooter>
              <CommandHint><FolderOpen size={15} /> No project selected</CommandHint>
              <CommandSubmit type="submit" disabled={!request.trim()}>
                Start <ArrowRight size={16} />
              </CommandSubmit>
            </CommandFooter>
          </CommandComposer>
          <Actions>
            <ActionButton type="button" $secondary onClick={(): void => onNavigate("projects")}>
              <FolderOpen size={16} weight="duotone" /> Browse projects
            </ActionButton>
            <ActionButton type="button" $secondary onClick={(): void => onOpenSettings("providers")}>
              <Cpu size={16} weight="duotone" /> Configure models
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
          <StatBody><span>Provider</span><strong>{configuredProvider?.displayName ?? "Not configured"}</strong></StatBody>
          <ArrowRight size={15} />
        </StatTile>
        <StatTile type="button" onClick={(): void => onNavigate("chat")}>
          <StatIcon><ChatCircleDots size={19} weight="duotone" /></StatIcon>
          <StatBody><span>Conversations</span><strong>{formatNumber(conversations.data?.length)}</strong></StatBody>
          <ArrowRight size={15} />
        </StatTile>
        <StatTile type="button" onClick={(): void => onOpenSettings("usage")}>
          <StatIcon><GraphicsCard size={19} weight="duotone" /></StatIcon>
          <StatBody><span>Tokens · 7 days</span><strong>{formatNumber(usage.data?.totals.totalTokens)}</strong></StatBody>
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
            {recentConversations.length > 0 ? (
              <ConversationList>
                {recentConversations.map((conversation: Conversation) => (
                  <ConversationRow key={conversation.id} type="button" onClick={(): void => onNavigate("chat")}>
                    <span>{conversation.title}</span>
                    <small>{conversation.selectedModel ?? "Choose a model"}</small>
                    <ArrowRight size={14} />
                  </ConversationRow>
                ))}
              </ConversationList>
            ) : (
              <EmptyState>
                <EmptyIcon><ChatCircleDots size={22} weight="duotone" /></EmptyIcon>
                <strong>No conversations yet</strong>
                <span>Start your first chat and it will show up here, private to your account.</span>
              </EmptyState>
            )}
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
              <Metric><span>Input</span><strong>{formatNumber(usage.data?.totals.inputTokens)}</strong></Metric>
              <Metric><span>Output</span><strong>{formatNumber(usage.data?.totals.outputTokens)}</strong></Metric>
              <Metric><span>Total</span><strong>{formatNumber(usage.data?.totals.totalTokens)}</strong></Metric>
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
            <ProviderState><StatusDot $online={Boolean(configuredProvider)} /> {configuredProvider ? "Ready" : "Not configured"}</ProviderState>
            <ProviderDetails>
              <span>Provider<strong>{configuredProvider?.displayName ?? "None selected"}</strong></span>
              <span>Model<strong>{configuredProvider?.selectedModel ?? "Unavailable"}</strong></span>
              <span>Type<strong>{configuredProvider?.providerType ?? "—"}</strong></span>
            </ProviderDetails>
            <PanelAction type="button" onClick={(): void => onOpenSettings("providers")}>
              Configure a provider <ArrowRight size={14} />
            </PanelAction>
          </Panel>
        </Stack>
      </Columns>

      <WorkspaceSection>
        <SectionHead>
          <div><Eyebrow>Workspace</Eyebrow><h2>Choose how you want to work</h2></div>
          <span>Unavailable capabilities stay visible and honest.</span>
        </SectionHead>
        <CapabilityGrid>
          <CapabilityItem type="button" $available onClick={(): void => onNavigate("chat")}>
            <ChatCircleDots size={22} weight="duotone" />
            <div><strong>Chat</strong><small>Private conversations with your configured models.</small><CapabilityTag $available>Available</CapabilityTag></div>
            <ArrowRight size={16} />
          </CapabilityItem>
          <CapabilityItem type="button" onClick={(): void => onNavigate("projects")}>
            <FolderOpen size={22} weight="duotone" />
            <div><strong>Projects</strong><small>Authorize a workspace for Nexo to inspect, edit, diff and validate.</small><CapabilityTag>Release 0.3</CapabilityTag></div>
            <ArrowRight size={16} />
          </CapabilityItem>
          <CapabilityItem type="button" onClick={(): void => onNavigate("chat")}>
            <Robot size={22} weight="duotone" />
            <div><strong>Project Agent</strong><small>Turn an objective into a visible plan, governed actions and verified results.</small><CapabilityTag>Release 0.4</CapabilityTag></div>
            <ArrowRight size={16} />
          </CapabilityItem>
          <CapabilityItem type="button" onClick={(): void => onNavigate("vaults")}>
            <Vault size={22} weight="duotone" />
            <div><strong>Knowledge Vaults</strong><small>Local knowledge, retrieval and cited answers.</small><CapabilityTag>Release 0.2</CapabilityTag></div>
            <ArrowRight size={16} />
          </CapabilityItem>
          <CapabilityItem type="button" onClick={(): void => onNavigate("skills")}>
            <Sparkle size={22} weight="duotone" />
            <div><strong>Skills</strong><small>Reusable, governed workflows for Agent.</small><CapabilityTag>Release 0.4</CapabilityTag></div>
            <ArrowRight size={16} />
          </CapabilityItem>
          <CapabilityItem type="button" onClick={(): void => onNavigate("cowork")}>
            <Brain size={22} weight="duotone" />
            <div><strong>Cowork</strong><small>Durable objectives, visible plans and checkpoints.</small><CapabilityTag>Release 0.6</CapabilityTag></div>
            <ArrowRight size={16} />
          </CapabilityItem>
          <CapabilityItem type="button" onClick={(): void => onNavigate("tasks")}>
            <CalendarCheck size={22} weight="duotone" />
            <div><strong>Tasks &amp; calendar</strong><small>Scheduled work, milestones and run history.</small><CapabilityTag>Release 0.6</CapabilityTag></div>
            <ArrowRight size={16} />
          </CapabilityItem>
        </CapabilityGrid>
      </WorkspaceSection>
    </Page>
  );
}
