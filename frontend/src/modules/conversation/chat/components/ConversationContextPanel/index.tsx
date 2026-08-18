import {
  CaretDoubleLeft,
  CaretDoubleRight,
  CheckCircle,
  ClipboardText,
  FileText,
  ImageSquare,
  ListChecks,
  type Icon
} from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import type {
  ConversationContextPanelProps,
  ConversationContextSection
} from "../../types/chatViewTypes";
import {
  CloseButton,
  EmptyCopy,
  EmptyIcon,
  EmptyState,
  Panel,
  PanelHeader,
  PanelTitle,
  PlanItem,
  PlanList,
  PlanMarker,
  PreviewBadge,
  Rail,
  RailButton,
  Tab,
  Tabs
} from "./styles";

type ContextTab = {
  id: ConversationContextSection;
  label: string;
  icon: Icon;
};

const contextTabs: ContextTab[] = [
  { id: "plan", label: "Plan", icon: ClipboardText },
  { id: "tasks", label: "Tasks", icon: ListChecks },
  { id: "artifacts", label: "Artifacts", icon: FileText },
  { id: "media", label: "Media", icon: ImageSquare }
];

const agentPreviewSteps: string[] = [
  "Understand the objective",
  "Request permissions",
  "Execute and verify"
];

export function ConversationContextPanel({
  mode,
  open,
  onOpenChange
}: ConversationContextPanelProps): ReactElement {
  const [section, setSection] = useState<ConversationContextSection>("plan");

  const selectSection = (nextSection: ConversationContextSection): void => {
    setSection(nextSection);
    onOpenChange(true);
  };

  const renderContent = (): ReactElement => {
    if (section === "tasks") {
      return (
        <EmptyState>
          <EmptyIcon><ListChecks size={22} weight="duotone" /></EmptyIcon>
          <EmptyCopy>
            <strong>No tasks yet</strong>
            <span>Agent tasks and their execution state will stay visible here.</span>
          </EmptyCopy>
        </EmptyState>
      );
    }

    if (section === "artifacts") {
      return (
        <EmptyState>
          <EmptyIcon><FileText size={22} weight="duotone" /></EmptyIcon>
          <EmptyCopy>
            <strong>No artifacts yet</strong>
            <span>Files, diffs and deliverables created in this conversation will appear here.</span>
          </EmptyCopy>
        </EmptyState>
      );
    }

    if (section === "media") {
      return (
        <EmptyState>
          <EmptyIcon><ImageSquare size={22} weight="duotone" /></EmptyIcon>
          <EmptyCopy>
            <strong>No media yet</strong>
            <span>Generated images and media will stay attached to this conversation.</span>
          </EmptyCopy>
        </EmptyState>
      );
    }

    if (mode === "agent") {
      return (
        <>
          <PreviewBadge>Runtime preview</PreviewBadge>
          <PlanList>
            {agentPreviewSteps.map((step: string, index: number) => (
              <PlanItem key={step}>
                <PlanMarker>
                  {index === 0 ? <CheckCircle size={16} weight="duotone" /> : index + 1}
                </PlanMarker>
                <span>{step}</span>
              </PlanItem>
            ))}
          </PlanList>
          <EmptyCopy>
            <span>The plan becomes live when the governed Agent runtime is connected.</span>
          </EmptyCopy>
        </>
      );
    }

    return (
      <EmptyState>
        <EmptyIcon><ClipboardText size={22} weight="duotone" /></EmptyIcon>
        <EmptyCopy>
          <strong>No active plan</strong>
          <span>Switch to Agent for multi-step work with a visible, governed plan.</span>
        </EmptyCopy>
      </EmptyState>
    );
  };

  if (!open) {
    return (
      <Rail aria-label="Conversation resources">
        <RailButton type="button" aria-label="Expand conversation resources" onClick={(): void => onOpenChange(true)}>
          <CaretDoubleLeft size={16} />
        </RailButton>
        {contextTabs.map((tab: ContextTab) => (
          <RailButton
            key={tab.id}
            type="button"
            aria-label={tab.label}
            title={tab.label}
            $active={section === tab.id}
            onClick={(): void => selectSection(tab.id)}
          >
            <tab.icon size={18} weight={section === tab.id ? "fill" : "duotone"} />
          </RailButton>
        ))}
      </Rail>
    );
  }

  return (
    <Panel aria-label="Conversation context">
      <PanelHeader>
        <div>
          <PanelTitle>Workspace</PanelTitle>
          <span>Plan, work and outputs</span>
        </div>
        <CloseButton type="button" aria-label="Minimize conversation resources" onClick={(): void => onOpenChange(false)}>
          <CaretDoubleRight size={16} />
        </CloseButton>
      </PanelHeader>
      <Tabs role="tablist" aria-label="Conversation resources">
        {contextTabs.map((tab: ContextTab) => (
          <Tab
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={section === tab.id}
            $active={section === tab.id}
            onClick={(): void => setSection(tab.id)}
          >
            <tab.icon size={15} weight={section === tab.id ? "fill" : "regular"} />
            {tab.label}
          </Tab>
        ))}
      </Tabs>
      {renderContent()}
    </Panel>
  );
}
