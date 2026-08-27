import { BookOpen, Buildings, Check, ChatCircleDots, Cpu, FolderOpen, LockKey, SpinnerGap, X } from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type ReactElement } from "react";
import { useNavigate, type NavigateFunction } from "react-router-dom";
import { Button } from "../../../../../shared/components/Button";
import { Loading } from "../../../../../shared/components/Loading";
import { ApiError } from "../../../../../shared/api/ApiError";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import { useProviderRegistry } from "../../../../provider/hooks/useProviderRegistry";
import { useProviderModelCatalogs } from "../../../../provider/hooks/useProviderModelCatalogs";
import { useBackendVaultCatalog } from "../../../../knowledge/vault/hooks/useBackendVaultCatalog";
import type { BackendVault } from "../../../../knowledge/vault/types/backendVaultTypes";
import { useMcpConnections } from "../../../../mcp/catalog/hooks/useMcpConnections";
import type { McpConnection, McpTool } from "../../../../mcp/catalog/types/mcpTypes";
import type { ProviderConfiguration, ProviderModel } from "../../../../provider/types/providerConfigurationTypes";
import { usePreferenceStore } from "../../../../settings/stores/usePreferenceStore";
import type { PreferenceState } from "../../../../settings/types/preferenceTypes";
import { useUsage } from "../../../../usage/hooks/useUsage";
import {
  useRefreshServerWorkspace,
  useServerWorkspaces,
  useServerWorkspaceStatus,
  useWorkspaceBindings
} from "../../../../project/workspace/hooks/useServerWorkspaces";
import type { ServerWorkspace, WorkspaceBinding } from "../../../../project/workspace/types/serverWorkspaceTypes";
import { useLocalWorkspacePicker } from "../../../../project/workspace/hooks/useLocalWorkspacePicker";
import { WorkspacePickerControl } from "../../../../project/workspace/components/WorkspacePickerControl";
import { ChatComposer } from "../../components/ChatComposer";
import { ConversationContextPanel } from "../../components/ConversationContextPanel";
import { ConversationSidebar } from "../../components/ConversationSidebar";
import { MessageList } from "../../components/MessageList";
import { ModelPicker } from "../../components/ModelPicker";
import {
  useArchiveConversation,
  useConversationMessages,
  useConversations,
  useCreateConversation,
  useRenameConversation,
  useSelectConversationKnowledge,
  useSelectConversationModel,
  useSelectConversationWorkspace
} from "../../hooks/useChat";
import { useChatStream } from "../../hooks/useChatStream";
import { useImageGeneration } from "../../../media/hooks/useImageGeneration";
import { parseContextualChatMessage } from "../../services/chatContextService";
import { useChatDraftStore } from "../../stores/useChatDraftStore";
import { useConversationModeStore } from "../../stores/useConversationModeStore";
import type { AgentPlan, AgentState, Conversation, ConversationMessage, ConversationMode, ToolExecution } from "../../types/chatTypes";
import type { ChatDraftState } from "../../types/chatDraftTypes";
import type { AgentContextSummary } from "../../types/chatViewTypes";
import {
  Chat,
  ChatContent,
  ConversationBody,
  ConversationColumn,
  Header,
  HeaderCopy,
  HeaderLeading,
  HeaderMeta,
  HeaderTitle,
  TitleEdit,
  TitleInput,
  Layout,
  LoadFailure,
  ModelArea,
  ModelLockNotice,
  NewConversationWorkspace,
  OpenConversations,
  PrivacyBadge,
  VaultBar,
  VaultBarLabel,
  VaultChip,
  WorkspaceServerNotice
} from "./styles";

export function ChatPage(): ReactElement {
  const navigate: NavigateFunction = useNavigate();
  const conversations = useConversations();
  const create = useCreateConversation();
  const rename = useRenameConversation();
  const archive = useArchiveConversation();
  const providers = useProviderRegistry();
  const backendVaults = useBackendVaultCatalog();
  const accountUsage = useUsage("ALL_TIME");
  const serverWorkspaces = useServerWorkspaces();
  const localWorkspacePicker = useLocalWorkspacePicker();

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isStartingNewConversation, setIsStartingNewConversation] = useState<boolean>(false);
  const mode: ConversationMode = useConversationModeStore((state) => state.mode);
  const setMode = useConversationModeStore((state) => state.setMode);
  const [isConversationMenuOpen, setIsConversationMenuOpen] = useState<boolean>((): boolean =>
    typeof window === "undefined"
    || typeof window.matchMedia !== "function"
    || !window.matchMedia("(max-width: 48rem)").matches
  );
  const [isContextOpen, setIsContextOpen] = useState<boolean>(false);
  const [draftModel, setDraftModel] = useState<{ providerConfigurationId: string; selectedModel: string } | null>(null);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [draftWorkspaceId, setDraftWorkspaceId] = useState<string | null>(null);
  const [isRenaming, setIsRenaming] = useState<boolean>(false);
  const [renameTitle, setRenameTitle] = useState<string>("");
  const initialDraft: string = useChatDraftStore((state: ChatDraftState) => state.content);
  const clearDraft: ChatDraftState["clear"] = useChatDraftStore((state: ChatDraftState) => state.clear);
  const thinkingEnabled: boolean = usePreferenceStore((state: PreferenceState) => state.thinkingEnabled);

  const messages = useConversationMessages(selectedId);
  const selectModel = useSelectConversationModel(selectedId);
  const selected: Conversation | undefined = conversations.data
    ?.find((item: Conversation) => item.id === selectedId);
  const selectedVaultIds: string[] = selected?.knowledgeVaultIds ?? [];
  const selectKnowledge = useSelectConversationKnowledge(selectedId);
  const selectWorkspace = useSelectConversationWorkspace(selectedId);
  const effectiveWorkspaceId: string | null = selected?.workspaceId ?? draftWorkspaceId;
  const activeWorkspace: ServerWorkspace | null = serverWorkspaces.data
    ?.find((workspace: ServerWorkspace) => workspace.id === effectiveWorkspaceId) ?? null;
  const workspaceStatus = useServerWorkspaceStatus(activeWorkspace?.id ?? null);
  const workspaceBindings = useWorkspaceBindings(activeWorkspace?.id ?? null);
  const activeBinding: WorkspaceBinding | undefined = workspaceBindings.data?.find(
    (binding: WorkspaceBinding): boolean => binding.id === selected?.workspaceBindingId
  ) ?? workspaceBindings.data?.find(
    (binding: WorkspaceBinding): boolean => binding.status === "AVAILABLE" || binding.status === "CHANGED"
  );
  const activeWorkspaceStatus: string | undefined = activeBinding?.status ?? workspaceStatus.data?.status;
  const refreshWorkspace = useRefreshServerWorkspace();
  const mcpConnections = useMcpConnections(mode === "agent");
  const imageGeneration = useImageGeneration(selectedId);
  const messageHistory: string[] = useMemo<string[]>(() => (messages.data ?? [])
    .filter((message: ConversationMessage): boolean => message.role === "USER")
    .map((message: ConversationMessage): string => parseContextualChatMessage(message.content).content)
    .filter((content: string): boolean => Boolean(content.trim())), [messages.data]);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);

  useEffect((): void => {
    if (!isStartingNewConversation && !selectedId && conversations.data?.[0]) {
      setSelectedId(conversations.data[0].id);
    }
  }, [conversations.data, isStartingNewConversation, selectedId]);

  useEffect((): void => {
    if (messages.error instanceof ApiError && messages.error.status === 404) {
      setIsStartingNewConversation(true);
      setSelectedId(null);
    }
  }, [messages.error]);

  useEffect((): void => {
    if (initialDraft) clearDraft();
  }, [clearDraft, initialDraft]);
  const configuredProviders: ProviderConfiguration[] = providers.registry.data
    ?.filter((provider: ProviderConfiguration) => provider.enabled) ?? [];
  const modelCatalogs = useProviderModelCatalogs(configuredProviders);
  const firstAvailableModel = modelCatalogs
    .flatMap((catalog) => catalog.models.map((model) => ({ providerConfigurationId: catalog.providerConfigurationId, selectedModel: model.name })))[0];
  const effectiveModel = selected?.selectedModel
    ? { providerConfigurationId: selected.providerConfigurationId ?? "", selectedModel: selected.selectedModel }
    : draftModel ?? firstAvailableModel ?? null;
  const hasModel: boolean = Boolean(effectiveModel?.selectedModel && effectiveModel.providerConfigurationId);
  const hasConfiguredProvider: boolean = configuredProviders.length > 0;
  const effectiveModelDetails: ProviderModel | undefined = modelCatalogs
    .find((catalog) => catalog.providerConfigurationId === effectiveModel?.providerConfigurationId)
    ?.models.find((model: ProviderModel) => model.name === effectiveModel?.selectedModel);
  const stream = useChatStream(
    selectedId,
    messages.data ?? [],
    mode,
    effectiveModelDetails?.thinkingSupported ?? null
  );
  const persistedAgentPlan: AgentPlan | null = useMemo<AgentPlan | null>(() =>
    (messages.data ?? []).findLast((message: ConversationMessage): boolean =>
      message.role === "ASSISTANT" && Boolean(message.agentPlan))?.agentPlan ?? null,
  [messages.data]);
  const visibleAgentPlan: AgentPlan | null = stream.agentPlan ?? persistedAgentPlan;
  const lastAssistantMessage: ConversationMessage | undefined = (messages.data ?? [])
    .findLast((message: ConversationMessage): boolean => message.role === "ASSISTANT");
  const visibleAgentState: AgentState | null = stream.agentState ?? lastAssistantMessage?.agentState ?? null;
  const visibleToolExecutions: ToolExecution[] = stream.toolExecutions.length > 0
    ? stream.toolExecutions
    : lastAssistantMessage?.toolExecutions ?? [];
  const selectedVaultNames: string[] = (backendVaults.vaults.data ?? [])
    .filter((vault: BackendVault) => selectedVaultIds.includes(vault.id))
    .map((vault: BackendVault) => vault.ownerType === "TEAM"
      ? `${vault.name} · Team ${vault.ownerName}`
      : vault.name);
  const enabledMcpConnections: McpConnection[] = (mcpConnections.data ?? [])
    .filter((connection: McpConnection) => connection.enabled);
  const agentContext: AgentContextSummary = {
    selectedVaultNames,
    enabledMcpConnectionNames: enabledMcpConnections.map((connection: McpConnection) => connection.displayName),
    enabledMcpToolCount: enabledMcpConnections.reduce((count: number, connection: McpConnection) =>
      count + connection.tools.filter((tool: McpTool) => tool.enabled).length, 0),
    knowledgeLoading: backendVaults.vaults.isLoading,
    knowledgeError: backendVaults.vaults.isError,
    mcpLoading: mcpConnections.isLoading,
    mcpError: mcpConnections.isError,
    modelToolCallingSupported: effectiveModelDetails?.toolCallingSupported ?? null,
    modelThinkingSupported: effectiveModelDetails?.thinkingSupported ?? null,
    thinkingEnabled,
    workspaceName: activeWorkspace?.name ?? null,
    workspaceStatus: activeBinding?.status ?? workspaceStatus.data?.status ?? activeWorkspace?.status ?? null,
    workspaceLoading: serverWorkspaces.isLoading || workspaceStatus.isLoading || workspaceBindings.isLoading,
    workspaceError: serverWorkspaces.isError || workspaceStatus.isError || workspaceBindings.isError
  };

  useEffect((): void => {
    if (!selectedId || selected?.selectedModel || selectModel.isPending) return;
    // When the conversation has no model yet, persist the user's draft, or otherwise the first
    // available model, so the selection shown at the top is the one the conversation actually uses.
    const modelToPersist = draftModel ?? firstAvailableModel;
    if (!modelToPersist) return;
    selectModel.mutate(modelToPersist, {
      onSuccess: (): void => setDraftModel(null)
    });
  }, [
    selectedId,
    selected?.selectedModel,
    selectModel,
    draftModel,
    firstAvailableModel?.providerConfigurationId,
    firstAvailableModel?.selectedModel
  ]);

  const createConversation = (title: string): void => {
    create.mutate({ title, workspaceId: draftWorkspaceId }, {
      onSuccess: (conversation: Conversation): void => {
        setIsStartingNewConversation(false);
        setSelectedId(conversation.id);
        setDraftWorkspaceId(null);
        if (typeof window.matchMedia === "function" && window.matchMedia("(max-width: 48rem)").matches) {
          setIsConversationMenuOpen(false);
        }
      }
    });
  };

  const titleFromMessage = (content: string): string => content.trim().split("\\n")[0]?.slice(0, 160) || "New conversation";

  const sendMessage = (content: string): void => {
    if (mode === "agent") setIsContextOpen(true);
    if (selectedId) {
      // Send immediately when the conversation already has a model; otherwise hold the message until
      // the auto-selected model has been persisted, then send it.
      if (selected?.selectedModel) stream.send(content);
      else setPendingMessage(content);
      return;
    }
    if (effectiveModel) setDraftModel(effectiveModel);
    setPendingMessage(content);
    createConversation(titleFromMessage(content));
  };

  useEffect((): void => {
    if (!pendingMessage || !selectedId || !selected?.selectedModel || stream.isBusy || selectModel.isPending) return;
    const content: string = pendingMessage;
    setPendingMessage(null);
    stream.send(content);
  }, [selected?.selectedModel, pendingMessage, selectedId, selectModel.isPending, stream]);

  const chooseModel = (providerConfigurationId: string, selectedModel: string): void => {
    if (!selectedId) {
      setDraftModel({ providerConfigurationId, selectedModel });
      return;
    }
    selectModel.mutate({ providerConfigurationId, selectedModel });
  };

  const toggleVault = (vaultId: string): void => {
    if (!selectedId || selectKnowledge.isPending) return;
    const nextSelection: string[] = selectedVaultIds.includes(vaultId)
      ? selectedVaultIds.filter((id: string): boolean => id !== vaultId)
      : [...selectedVaultIds, vaultId];
    selectKnowledge.mutate(nextSelection);
  };

  const chooseLocalFolder = (): void => {
    localWorkspacePicker.chooseLocalWorkspace()
      .then((workspace: ServerWorkspace | null): Promise<unknown> | void => {
        if (!workspace) return;
        if (!selectedId) {
          setDraftWorkspaceId(workspace.id);
          return;
        }
        setDraftWorkspaceId(null);
        return selectWorkspace.mutateAsync(workspace.id);
      })
      .catch(() => undefined);
  };

  const saveRename = (): void => {
    if (!selectedId || !renameTitle.trim()) return;
    rename.mutate({ conversationId: selectedId, title: renameTitle.trim() }, {
      onSuccess: (): void => setIsRenaming(false)
    });
  };

  const archiveConversation = (conversation: Conversation): void => {
    ask({
      title: "Archive this conversation?",
      message: `"${conversation.title}" will be removed from your list. Its history stays stored.`,
      confirmLabel: "Archive",
      tone: "danger"
    }).then((confirmed: boolean): void => {
      if (!confirmed) return;

      archive.mutate(conversation.id, {
        onSuccess: (): void => {
          if (conversation.id === selectedId) setSelectedId(null);
        }
      });
    });
  };

  if (conversations.isLoading) return <Loading label="Loading your conversations…" />;

  if (conversations.isError) {
    return (
      <LoadFailure>
        <p>We could not load your conversations.</p>
        <Button type="button" variant="outline" onClick={(): void => { conversations.refetch(); }}>
          Try again
        </Button>
      </LoadFailure>
    );
  }

  return (
    <Layout $sidebarOpen={isConversationMenuOpen}>
      {isConversationMenuOpen && (
        <ConversationSidebar
          conversations={conversations.data ?? []}
          selectedId={selectedId}
          isCreating={create.isPending}
          onSelect={(conversationId: string): void => {
            setIsStartingNewConversation(false);
            setDraftWorkspaceId(null);
            setSelectedId(conversationId);
          }}
          onNew={(): void => {
            setIsStartingNewConversation(true);
            setSelectedId(null);
            setDraftModel(null);
            setDraftWorkspaceId(null);
            setPendingMessage(null);
            if (typeof window.matchMedia === "function" && window.matchMedia("(max-width: 48rem)").matches) setIsConversationMenuOpen(false);
          }}
          onArchive={archiveConversation}
          onRename={(conversation: Conversation): void => {
            setSelectedId(conversation.id);
            setRenameTitle(conversation.title);
            setIsRenaming(true);
          }}
          onClose={(): void => setIsConversationMenuOpen(false)}
        />
      )}

      <Chat>
        <Header>
          <HeaderLeading>
            {!isConversationMenuOpen && (
              <OpenConversations
                type="button"
                aria-label="Open conversation menu"
                title="Open conversations"
                onClick={(): void => setIsConversationMenuOpen(true)}
              >
                <ChatCircleDots size={18} weight="duotone" />
              </OpenConversations>
            )}
            <HeaderCopy>
              {isRenaming ? (
                <TitleEdit>
                  <TitleInput value={renameTitle} maxLength={160} autoFocus onChange={(event): void => setRenameTitle(event.target.value)} onKeyDown={(event): void => { if (event.key === "Enter") saveRename(); if (event.key === "Escape") setIsRenaming(false); }} />
                  <Button type="button" variant="outline" icon={Check} aria-label="Save conversation title" onClick={saveRename} disabled={rename.isPending} />
                  <Button type="button" variant="outline" icon={X} aria-label="Cancel title editing" onClick={(): void => setIsRenaming(false)} />
                </TitleEdit>
              ) : <HeaderTitle>{selected?.title ?? "Start a conversation"}</HeaderTitle>}
              <HeaderMeta>
                <PrivacyBadge title="Private to your account"><LockKey size={12} weight="bold" /> Private</PrivacyBadge>
                {selectedId && (
                  <WorkspacePickerControl
                    workspaceId={effectiveWorkspaceId}
                    workspaces={serverWorkspaces.data ?? []}
                    selectDisabled={serverWorkspaces.isLoading || selectWorkspace.isPending || stream.isBusy}
                    localDisabled={stream.isBusy}
                    localAvailable={localWorkspacePicker.available}
                    localPending={localWorkspacePicker.pending}
                    onSelect={(workspaceId: string | null): void => {
                      setDraftWorkspaceId(null);
                      selectWorkspace.mutate(workspaceId);
                    }}
                    onChooseLocal={chooseLocalFolder}
                  />
                )}
                {selected?.selectedModel && <span><Cpu size={12} /> Local</span>}
              </HeaderMeta>
            </HeaderCopy>
          </HeaderLeading>
          <ModelArea>
            {stream.isBusy && (
              <ModelLockNotice aria-live="polite">
                <SpinnerGap size={12} weight="bold" /> Nexo is thinking · model locked
              </ModelLockNotice>
            )}
            <ModelPicker
              catalogs={modelCatalogs}
              selectedProviderId={effectiveModel?.providerConfigurationId ?? null}
              selectedModel={effectiveModel?.selectedModel ?? null}
              disabled={stream.isBusy}
              isSaving={selectModel.isPending}
              errorMessage={selectModel.error?.message ?? null}
              onSelect={(providerConfigurationId: string, selectedModel: string): void =>
                chooseModel(providerConfigurationId, selectedModel)}
            />
          </ModelArea>
        </Header>

        <ChatContent>
          {(localWorkspacePicker.error || selectWorkspace.isError) && (
            <WorkspaceServerNotice role="alert">
              <span>
                <strong>Workspace selection needs attention</strong>
                {localWorkspacePicker.error ?? selectWorkspace.error?.message ?? "Nexo could not select this workspace."}
              </span>
              <Button type="button" variant="outline" onClick={(): void => { void navigate("/projects"); }}>Manage</Button>
            </WorkspaceServerNotice>
          )}
          {activeWorkspace && activeWorkspaceStatus && activeWorkspaceStatus !== "AVAILABLE" && (
            <WorkspaceServerNotice role="status">
              <span>
                <strong>{activeWorkspace.name}: {activeWorkspaceStatus.toLowerCase()}</strong>
                {activeBinding
                  ? activeWorkspaceStatus === "CHANGED"
                    ? "The project structure changed on the paired computer. Review it before relying on the previous context."
                    : "The paired computer or local folder is not currently available to Nexo Agent."
                  : workspaceStatus.data?.reason ?? (activeWorkspaceStatus === "CHANGED"
                    ? "The project structure changed on the server. Refresh before relying on the previous scan."
                    : "This project is not currently readable by the Nexo server.")}
              </span>
              {!activeBinding && activeWorkspaceStatus === "CHANGED" ? (
                <Button
                  type="button"
                  variant="outline"
                  disabled={refreshWorkspace.isPending}
                  onClick={(): void => refreshWorkspace.mutate(activeWorkspace.id)}
                >Refresh context</Button>
              ) : (
                <Button type="button" variant="outline" onClick={(): void => { void navigate("/projects"); }}>Manage</Button>
              )}
            </WorkspaceServerNotice>
          )}
          <ConversationBody $contextOpen={isContextOpen}>
            <ConversationColumn>
              <MessageList
                messages={messages.data ?? []}
                isLoading={messages.isLoading}
                hasConversation={Boolean(selectedId)}
                hasModel={hasModel}
                hasConfiguredProvider={hasConfiguredProvider}
                phase={stream.phase}
                startedAt={stream.startedAt}
                thinkingContent={stream.thinkingContent}
                streamingContent={stream.streamingContent}
                errorMessage={stream.errorMessage}
                agentState={stream.agentState}
                toolExecutions={stream.toolExecutions}
                accountTokenTotal={accountUsage.data?.totals.totalTokens ?? null}
                mode={mode}
                startAccessory={(
                  <NewConversationWorkspace aria-label="New conversation workspace">
                    <div>
                      <FolderOpen size={22} weight="duotone" />
                      <span>
                        <strong>Workspace for this chat</strong>
                        <small>Select an existing workspace or open a project folder. The first message will already use this context.</small>
                      </span>
                    </div>
                    <WorkspacePickerControl
                      workspaceId={draftWorkspaceId}
                      workspaces={serverWorkspaces.data ?? []}
                      selectDisabled={serverWorkspaces.isLoading || create.isPending}
                      localDisabled={create.isPending}
                      localAvailable={localWorkspacePicker.available}
                      localPending={localWorkspacePicker.pending}
                      onSelect={setDraftWorkspaceId}
                      onChooseLocal={chooseLocalFolder}
                    />
                  </NewConversationWorkspace>
                )}
                onConfigureProvider={(): void => { navigate("/settings/providers"); }}
              />

              {(backendVaults.vaults.data ?? []).length > 0 && (
                <VaultBar aria-busy={selectKnowledge.isPending}>
                  <VaultBarLabel><BookOpen size={13} weight="duotone" /> Knowledge</VaultBarLabel>
                  {(backendVaults.vaults.data ?? []).map((vault: BackendVault) => (
                    <VaultChip
                      key={vault.id}
                      type="button"
                      $active={selectedVaultIds.includes(vault.id)}
                      aria-pressed={selectedVaultIds.includes(vault.id)}
                      disabled={!selectedId || selectKnowledge.isPending}
                      onClick={(): void => toggleVault(vault.id)}
                    >
                      {selectedVaultIds.includes(vault.id) && <Check size={12} weight="bold" />}
                      {vault.ownerType === "TEAM" && <Buildings size={12} weight="duotone" />}
                      <span>{vault.name}{vault.ownerType === "TEAM" && <small>{vault.ownerName}</small>}</span>
                    </VaultChip>
                  ))}
                  {selectKnowledge.isPending && <VaultBarLabel>Saving…</VaultBarLabel>}
                  {selectKnowledge.isError && (
                    <VaultBarLabel role="alert">Could not save Vault selection</VaultBarLabel>
                  )}
                </VaultBar>
              )}

              <ChatComposer
                initialContent={initialDraft}
                messageHistory={messageHistory}
                disabled={create.isPending || selectWorkspace.isPending}
                hasModel={hasModel}
                phase={stream.phase}
                isBusy={stream.isBusy}
                imageRuntimeAvailable={Boolean(selectedId) && imageGeneration.runtime.data?.available === true}
                imageRuntimeMessage={imageGeneration.runtime.data?.message
                  ?? (imageGeneration.runtime.isLoading
                    ? "Checking the local ComfyUI runtime…"
                    : "The local ComfyUI runtime is unavailable")}
                imageModels={imageGeneration.runtime.data?.models ?? []}
                defaultImageModel={imageGeneration.runtime.data?.model ?? null}
                imageSubmitting={imageGeneration.generate.isPending}
                mode={mode}
                agentContext={agentContext}
                workspace={activeWorkspace}
                onModeChange={setMode}
                onInspectKnowledge={(): void => setIsContextOpen(true)}
                onManageMcp={(): void => { navigate("/mcp"); }}
                onSend={sendMessage}
                onGenerateImage={(prompt: string, model: string): void => {
                  setIsContextOpen(true);
                  imageGeneration.generate.mutate({ prompt, model });
                }}
                onCancel={stream.cancel}
              />
            </ConversationColumn>
            <ConversationContextPanel
              conversationId={selectedId}
              mode={mode}
              agentPlan={visibleAgentPlan}
              agentState={visibleAgentState}
              toolExecutions={visibleToolExecutions}
              open={isContextOpen}
              vaults={backendVaults.vaults.data ?? []}
              selectedVaultIds={selectedVaultIds}
              isVaultSelectionPending={selectKnowledge.isPending}
              vaultSelectionError={selectKnowledge.error?.message ?? null}
              onOpenChange={setIsContextOpen}
              onToggleVault={toggleVault}
              onManageVaults={(): void => { navigate("/vaults"); }}
              onManageWorkspace={(): void => { navigate("/projects"); }}
              workspaceId={selected?.workspaceId ?? null}
              workspaceBindingId={selected?.workspaceBindingId ?? null}
            />
          </ConversationBody>
        </ChatContent>
      </Chat>

    </Layout>
  );
}
