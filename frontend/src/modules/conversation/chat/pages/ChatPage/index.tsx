import { BookOpen, Check, ChatCircleDots, Cpu, FolderOpen, LockKey, SpinnerGap, X } from "@phosphor-icons/react";
import { useEffect, useMemo, useRef, useState, type ReactElement } from "react";
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
import type { ProviderConfiguration } from "../../../../provider/types/providerConfigurationTypes";
import { useUsage } from "../../../../usage/hooks/useUsage";
import { WorkspaceChangeNotice } from "../../../../project/workspace/components/WorkspaceChangeNotice";
import { useActiveWorkspace } from "../../../../project/workspace/hooks/useActiveWorkspace";
import { useWorkspaceRegistration } from "../../../../project/workspace/hooks/useWorkspaceRegistration";
import { useWorkspaceCheck } from "../../../../project/workspace/hooks/useWorkspaceCheck";
import { useWorkspaceStore } from "../../../../project/workspace/stores/useWorkspaceStore";
import type { WorkspaceCheck, WorkspaceState } from "../../../../project/workspace/types/workspaceTypes";
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
  useSelectConversationModel
} from "../../hooks/useChat";
import { useChatStream } from "../../hooks/useChatStream";
import { parseContextualChatMessage } from "../../services/chatContextService";
import { useChatDraftStore } from "../../stores/useChatDraftStore";
import type { Conversation, ConversationMessage, ConversationMode } from "../../types/chatTypes";
import type { ChatDraftState } from "../../types/chatDraftTypes";
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
  OpenConversations,
  PrivacyBadge,
  VaultBar,
  VaultBarLabel,
  VaultChip,
  WorkspaceContext
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
  const activeWorkspace = useActiveWorkspace();
  const workspaceRegistration = useWorkspaceRegistration();
  const workspaceCheck: WorkspaceCheck = useWorkspaceStore((state: WorkspaceState) => state.workspaceCheck);
  const workspaceHydration: WorkspaceState["hydrationStatus"] = useWorkspaceStore((state: WorkspaceState) => state.hydrationStatus);
  const skipNextWorkspaceCheck: boolean = useWorkspaceStore((state: WorkspaceState) => state.skipNextWorkspaceCheck);
  const consumeWorkspaceCheckSkip: WorkspaceState["consumeWorkspaceCheckSkip"] = useWorkspaceStore((state: WorkspaceState) => state.consumeWorkspaceCheckSkip);
  const workspaceInspection = useWorkspaceCheck();
  const inspectedWorkspaceId = useRef<string | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isStartingNewConversation, setIsStartingNewConversation] = useState<boolean>(false);
  const [mode, setMode] = useState<ConversationMode>("chat");
  const [isConversationMenuOpen, setIsConversationMenuOpen] = useState<boolean>((): boolean =>
    typeof window === "undefined"
    || typeof window.matchMedia !== "function"
    || !window.matchMedia("(max-width: 48rem)").matches
  );
  const [isContextOpen, setIsContextOpen] = useState<boolean>(false);
  const [draftModel, setDraftModel] = useState<{ providerConfigurationId: string; selectedModel: string } | null>(null);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [isRenaming, setIsRenaming] = useState<boolean>(false);
  const [renameTitle, setRenameTitle] = useState<string>("");
  const initialDraft: string = useChatDraftStore((state: ChatDraftState) => state.content);
  const clearDraft: ChatDraftState["clear"] = useChatDraftStore((state: ChatDraftState) => state.clear);

  const messages = useConversationMessages(selectedId);
  const selectModel = useSelectConversationModel(selectedId);
  const selected: Conversation | undefined = conversations.data
    ?.find((item: Conversation) => item.id === selectedId);
  const selectedVaultIds: string[] = selected?.knowledgeVaultIds ?? [];
  const selectKnowledge = useSelectConversationKnowledge(selectedId);
  const stream = useChatStream(selectedId, messages.data ?? [], mode);
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


  useEffect((): void => {
    if (
      workspaceHydration === "ready"
      && activeWorkspace
      && inspectedWorkspaceId.current !== activeWorkspace.id
    ) {
      inspectedWorkspaceId.current = activeWorkspace.id;
      if (skipNextWorkspaceCheck) {
        consumeWorkspaceCheckSkip();
        return;
      }
      workspaceInspection.checkActiveWorkspace(false);
    }
  }, [activeWorkspace, consumeWorkspaceCheckSkip, skipNextWorkspaceCheck, workspaceHydration, workspaceInspection]);

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
    create.mutate(title, {
      onSuccess: (conversation: Conversation): void => {
        setIsStartingNewConversation(false);
        setSelectedId(conversation.id);
        if (typeof window.matchMedia === "function" && window.matchMedia("(max-width: 48rem)").matches) {
          setIsConversationMenuOpen(false);
        }
      }
    });
  };

  const titleFromMessage = (content: string): string => content.trim().split("\\n")[0]?.slice(0, 160) || "New conversation";

  const sendMessage = (content: string): void => {
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
            setSelectedId(conversationId);
          }}
          onNew={(): void => {
            setIsStartingNewConversation(true);
            setSelectedId(null);
            setDraftModel(null);
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
                <WorkspaceContext
                  type="button"
                  $active={Boolean(activeWorkspace)}
                  title={activeWorkspace ? `Local workspace: ${activeWorkspace.directoryName}` : "Choose a workspace folder"}
                  onClick={(): void => {
                    if (!activeWorkspace && workspaceRegistration.isSupported) {
                      void workspaceRegistration.chooseFolder("read");
                      return;
                    }
                    navigate("/projects");
                  }}
                >
                  <FolderOpen size={15} weight={activeWorkspace ? "fill" : "duotone"} />
                  Workspace: {workspaceRegistration.isPicking ? "Opening folder…" : activeWorkspace?.name ?? "Choose workspace"}
                </WorkspaceContext>
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
          {activeWorkspace && (
            <WorkspaceChangeNotice
              check={workspaceCheck}
              workspaceName={activeWorkspace.name}
              onManage={(): void => { navigate("/projects"); }}
              onRecheck={(): void => { workspaceInspection.checkActiveWorkspace(true); }}
              onAccept={(): void => { workspaceInspection.acceptCurrentStructure(); }}
            />
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
                agentPlan={stream.agentPlan}
                toolExecutions={stream.toolExecutions}
                accountTokenTotal={accountUsage.data?.totals.totalTokens ?? null}
                mode={mode}
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
                      {vault.name}
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
                disabled={false}
                hasModel={hasModel}
                phase={stream.phase}
                isBusy={stream.isBusy}
                mode={mode}
                onModeChange={setMode}
                onSend={sendMessage}
                onCancel={stream.cancel}
              />
            </ConversationColumn>
            <ConversationContextPanel
              conversationId={selectedId}
              mode={mode}
              open={isContextOpen}
              vaults={backendVaults.vaults.data ?? []}
              selectedVaultIds={selectedVaultIds}
              isVaultSelectionPending={selectKnowledge.isPending}
              vaultSelectionError={selectKnowledge.error?.message ?? null}
              onOpenChange={setIsContextOpen}
              onToggleVault={toggleVault}
              onManageVaults={(): void => { navigate("/vaults"); }}
              onManageWorkspace={(): void => { navigate("/projects"); }}
            />
          </ConversationBody>
        </ChatContent>
      </Chat>

    </Layout>
  );
}
