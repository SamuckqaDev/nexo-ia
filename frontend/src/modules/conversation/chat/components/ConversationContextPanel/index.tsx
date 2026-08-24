import {
  CaretDoubleLeft,
  CaretDoubleRight,
  Brain,
  ClipboardText,
  FileText,
  FolderOpen,
  ImageSquare,
  ListChecks,
  Paperclip,
  Trash,
  Vault,
  type Icon
} from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { AgentPlan } from "../AgentPlan";
import { ImageGenerationProgress } from "../../../media/components/ImageGenerationProgress";
import { useImageGenerationStore } from "../../../media/stores/useImageGenerationStore";
import type { ImageGenerationJob, ImageGenerationState } from "../../../media/types/imageGenerationTypes";
import { useVaultSources } from "../../../../knowledge/vault/hooks/useVaultSources";
import type { BackendSource, BackendVault } from "../../../../knowledge/vault/types/backendVaultTypes";
import { usePersonalMemories } from "../../../../memory/personal/hooks/usePersonalMemories";
import type { PersonalMemory } from "../../../../memory/personal/types/personalMemoryTypes";
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
  MemoryCard,
  MemoryRemoveButton,
  Panel,
  PanelHeader,
  PanelTitle,
  Rail,
  RailButton,
  ResourceCard,
  ResourceList,
  SectionAction,
  SourceButton,
  StatusCopy,
  Tab,
  Tabs,
  VaultSourceList,
  VaultSourceRow
} from "./styles";

type ContextTab = {
  id: ConversationContextSection;
  label: string;
  icon: Icon;
};

const contextTabs: ContextTab[] = [
  { id: "workspace", label: "Project", icon: FolderOpen },
  { id: "vaults", label: "Vaults", icon: Vault },
  { id: "memory", label: "Memory", icon: Brain },
  { id: "plan", label: "Plan", icon: ClipboardText },
  { id: "tasks", label: "Tasks", icon: ListChecks },
  { id: "artifacts", label: "Artifacts", icon: FileText },
  { id: "media", label: "Media", icon: ImageSquare }
];

type VaultContextCardProps = {
  vault: BackendVault;
  selected: boolean;
  disabled: boolean;
  onToggle: () => void;
};

function VaultContextCard({ vault, selected, disabled, onToggle }: VaultContextCardProps): ReactElement {
  const sourceCatalog = useVaultSources(vault.id);
  const sources: BackendSource[] = sourceCatalog.sources.data ?? [];

  return (
    <ResourceCard>
      <header>
        <span><Vault size={17} weight="duotone" />{vault.name}</span>
        <small>{selected ? "In retrieval" : vault.scope.toLowerCase()}</small>
      </header>
      <SourceButton
        type="button"
        $active={selected}
        aria-pressed={selected}
        disabled={disabled}
        onClick={onToggle}
      >
        <Paperclip size={14} weight={selected ? "fill" : "regular"} />
        <span>
          <strong>{selected ? "Remove from Chat" : "Use this Vault"}</strong>
          <small>{selected ? "Nexo searches it for new messages" : "Enable server-side semantic retrieval"}</small>
        </span>
      </SourceButton>
      {sourceCatalog.sources.isLoading ? (
        <StatusCopy>Loading indexed documents…</StatusCopy>
      ) : sourceCatalog.sources.isError ? (
        <StatusCopy>Nexo could not load this Vault's documents.</StatusCopy>
      ) : sources.length ? (
        <VaultSourceList>
          {sources.map((source: BackendSource) => (
            <VaultSourceRow key={source.id}>
              <FileText size={13} weight="duotone" />
              <span title={source.displayName}>{source.displayName}</span>
              <small>{source.status.toLowerCase()}</small>
            </VaultSourceRow>
          ))}
        </VaultSourceList>
      ) : <StatusCopy>This Vault has no documents.</StatusCopy>}
    </ResourceCard>
  );
}

export function ConversationContextPanel({
  conversationId,
  mode,
  agentPlan,
  open,
  vaults,
  selectedVaultIds,
  isVaultSelectionPending,
  vaultSelectionError,
  onOpenChange,
  onToggleVault,
  onManageVaults,
  onManageWorkspace
}: ConversationContextPanelProps): ReactElement {
  const [section, setSection] = useState<ConversationContextSection>("workspace");
  const activeWorkspace = useActiveWorkspace();
  const workspaceSnapshot = useWorkspaceSnapshot(activeWorkspace?.id ?? null);
  const personalMemories = usePersonalMemories(Boolean(conversationId));
  const imageJobs: ImageGenerationJob[] = Object.values(useImageGenerationStore(
    (state: ImageGenerationState) => state.jobs))
    .filter((job: ImageGenerationJob): boolean => job.conversationId === conversationId);

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
          <StatusCopy>Select the real Vaults Nexo may search for each new message. Retrieval stays isolated to your authenticated account.</StatusCopy>
          {isVaultSelectionPending && <StatusCopy aria-live="polite">Saving this conversation's Vault selection…</StatusCopy>}
          {vaultSelectionError && <StatusCopy role="alert">{vaultSelectionError}</StatusCopy>}
          {vaults.length ? vaults.map((vault: BackendVault) => (
            <VaultContextCard
              key={vault.id}
              vault={vault}
              selected={selectedVaultIds.includes(vault.id)}
              disabled={!conversationId || isVaultSelectionPending}
              onToggle={(): void => onToggleVault(vault.id)}
            />
          )) : <StatusCopy>No Knowledge Vaults are available for this account.</StatusCopy>}
          <SectionAction><Button type="button" variant="outline" onClick={onManageVaults}>Open Vault Explorer</Button></SectionAction>
        </ResourceList>
      );
    }

    if (section === "memory") {
      if (personalMemories.memories.isLoading) {
        return <StatusCopy>Loading your personal memories…</StatusCopy>;
      }
      if (personalMemories.memories.isError) {
        return <StatusCopy role="alert">Nexo could not load your personal memories.</StatusCopy>;
      }
      const memories: PersonalMemory[] = personalMemories.memories.data ?? [];
      return (
        <ResourceList>
          <StatusCopy>
            These notes belong only to your account and may follow you across conversations.
            In Agent mode, ask Nexo to remember a stable preference or fact.
          </StatusCopy>
          {memories.length ? memories.map((memory: PersonalMemory) => (
            <MemoryCard key={memory.id}>
              <Brain size={15} weight="duotone" aria-hidden />
              <span>{memory.content}</span>
              <MemoryRemoveButton
                type="button"
                aria-label={`Remove memory: ${memory.content}`}
                title="Remove memory"
                disabled={personalMemories.remove.isPending}
                onClick={(): void => personalMemories.remove.mutate(memory.id)}
              >
                <Trash size={13} />
              </MemoryRemoveButton>
            </MemoryCard>
          )) : (
            <EmptyState>
              <EmptyIcon><Brain size={22} weight="duotone" /></EmptyIcon>
              <EmptyCopy><strong>No personal memories yet</strong><span>Use Agent and explicitly ask Nexo to remember something for future chats.</span></EmptyCopy>
            </EmptyState>
          )}
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
      if (imageJobs.length > 0) {
        return (
          <ResourceList>
            <StatusCopy>Image progress is reported by the runtime when available; otherwise Nexo shows an honest indeterminate state and elapsed time.</StatusCopy>
            {imageJobs.map((job: ImageGenerationJob) => <ImageGenerationProgress key={job.id} job={job} />)}
          </ResourceList>
        );
      }
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

    if (agentPlan) {
      return (
        <>
          <AgentPlan plan={agentPlan} />
          <EmptyCopy>
            <span>This is the latest persisted revision for this conversation and it updates while Agent is working.</span>
          </EmptyCopy>
        </>
      );
    }

    return (
      <EmptyState>
        <EmptyIcon><ClipboardText size={22} weight="duotone" /></EmptyIcon>
        <EmptyCopy>
          <strong>No active plan</strong>
          <span>{mode === "agent"
            ? "Send an Agent request to create a visible, governed implementation plan."
            : "Switch to Agent for multi-step work with a visible, governed plan."}</span>
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
