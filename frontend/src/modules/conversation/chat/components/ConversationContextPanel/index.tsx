import {
  CaretDoubleLeft,
  CaretDoubleRight,
  CheckCircle,
  ClipboardText,
  FileText,
  FolderOpen,
  ImageSquare,
  ListChecks,
  Paperclip,
  Vault,
  type Icon
} from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { useVaultCatalogStore } from "../../../../knowledge/vault/stores/useVaultCatalogStore";
import type { KnowledgeVault, VaultCatalogState, VaultSource } from "../../../../knowledge/vault/types/vaultTypes";
import { WorkspaceTree } from "../../../../project/workspace/components/WorkspaceTree";
import { useActiveWorkspace } from "../../../../project/workspace/hooks/useActiveWorkspace";
import { useWorkspaceSnapshot } from "../../../../project/workspace/hooks/useWorkspaceSnapshot";
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
  ResourceCard,
  ResourceList,
  SectionAction,
  SourceButton,
  StatusCopy,
  Tab,
  Tabs
} from "./styles";

type ContextTab = {
  id: ConversationContextSection;
  label: string;
  icon: Icon;
};

const contextTabs: ContextTab[] = [
  { id: "workspace", label: "Project", icon: FolderOpen },
  { id: "vaults", label: "Vaults", icon: Vault },
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
  onOpenChange,
  onManageVaults,
  onManageWorkspace
}: ConversationContextPanelProps): ReactElement {
  const [section, setSection] = useState<ConversationContextSection>("workspace");
  const activeWorkspace = useActiveWorkspace();
  const workspaceSnapshot = useWorkspaceSnapshot(activeWorkspace?.id ?? null);
  const vaults: KnowledgeVault[] = useVaultCatalogStore((state: VaultCatalogState) => state.vaults);
  const attachedSourceIds: string[] = useVaultCatalogStore((state: VaultCatalogState) => state.attachedSourceIds);
  const toggleSourceAttachment: VaultCatalogState["toggleSourceAttachment"] = useVaultCatalogStore((state: VaultCatalogState) => state.toggleSourceAttachment);

  const selectSection = (nextSection: ConversationContextSection): void => {
    setSection(nextSection);
    onOpenChange(true);
  };

  const renderContent = (): ReactElement => {
    if (section === "workspace") {
      if (!activeWorkspace) {
        return (
          <EmptyState>
            <EmptyIcon><FolderOpen size={22} weight="duotone" /></EmptyIcon>
            <EmptyCopy><strong>No project selected</strong><span>Choose a local workspace to inspect its saved folder structure here.</span></EmptyCopy>
            <Button type="button" variant="outline" onClick={onManageWorkspace}>Choose workspace</Button>
          </EmptyState>
        );
      }
      return (
        <ResourceList>
          <ResourceCard>
            <header><span><FolderOpen size={17} weight="fill" />{activeWorkspace.name}</span><small>{activeWorkspace.access}</small></header>
            {workspaceSnapshot.status === "loading" && <StatusCopy>Loading saved project structure…</StatusCopy>}
            {workspaceSnapshot.status === "error" && <StatusCopy>Nexo could not read this structure from local browser storage.</StatusCopy>}
            {workspaceSnapshot.status === "ready" && workspaceSnapshot.snapshot && <WorkspaceTree snapshot={workspaceSnapshot.snapshot} compact />}
            {workspaceSnapshot.status === "ready" && !workspaceSnapshot.snapshot && <StatusCopy>No structure snapshot is available.</StatusCopy>}
          </ResourceCard>
          <SectionAction><Button type="button" variant="outline" onClick={onManageWorkspace}>Manage workspace</Button></SectionAction>
        </ResourceList>
      );
    }

    if (section === "vaults") {
      return (
        <ResourceList>
          <StatusCopy>Attach readable text sources here. Their bounded excerpts are added to new Chat messages until removed; excerpts already sent remain in conversation history.</StatusCopy>
          {vaults.map((vault: KnowledgeVault) => (
            <ResourceCard key={vault.id}>
              <header><span><Vault size={17} weight="duotone" />{vault.name}</span><small>{vault.sources.length} sources</small></header>
              {vault.sources.length ? vault.sources.map((source: VaultSource) => {
                const attached: boolean = attachedSourceIds.includes(source.id);
                return (
                  <SourceButton
                    key={source.id}
                    type="button"
                    $active={attached}
                    disabled={!source.contentPreview}
                    aria-label={source.contentPreview ? `${attached ? "Remove" : "Attach"} ${source.name} ${attached ? "from" : "to"} Chat` : `${source.name} has no readable preview`}
                    onClick={(): void => toggleSourceAttachment(source.id)}
                  >
                    <Paperclip size={14} weight={attached ? "fill" : "regular"} />
                    <span><strong>{source.name}</strong><small>{source.contentPreview ? attached ? "Attached to Chat" : "Available text excerpt" : "Metadata only"}</small></span>
                  </SourceButton>
                );
              }) : <StatusCopy>This Vault has no sources.</StatusCopy>}
            </ResourceCard>
          ))}
          <SectionAction><Button type="button" variant="outline" onClick={onManageVaults}>Open Vault Explorer</Button></SectionAction>
        </ResourceList>
      );
    }

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
          <span>Project, knowledge and outputs</span>
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
