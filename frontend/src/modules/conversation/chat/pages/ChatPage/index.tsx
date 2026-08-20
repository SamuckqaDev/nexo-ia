import { Cpu, FolderOpen, LockKey } from "@phosphor-icons/react";
import { useEffect, useState, type ReactElement } from "react";
import { useNavigate, type NavigateFunction } from "react-router-dom";
import { Button } from "../../../../../shared/components/Button";
import { Loading } from "../../../../../shared/components/Loading";
import { useConfirmationStore } from "../../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../../shared/feedback/types/confirmationTypes";
import { useProviderRegistry } from "../../../../provider/hooks/useProviderRegistry";
import type { ProviderConfiguration } from "../../../../provider/types/providerConfigurationTypes";
import { WorkspaceChangeNotice } from "../../../../project/workspace/components/WorkspaceChangeNotice";
import { useActiveWorkspace } from "../../../../project/workspace/hooks/useActiveWorkspace";
import { useWorkspaceCheck } from "../../../../project/workspace/hooks/useWorkspaceCheck";
import { useWorkspaceStore } from "../../../../project/workspace/stores/useWorkspaceStore";
import type { WorkspaceCheck, WorkspaceState } from "../../../../project/workspace/types/workspaceTypes";
import { ChatComposer } from "../../components/ChatComposer";
import { ConversationContextPanel } from "../../components/ConversationContextPanel";
import { ConversationSidebar } from "../../components/ConversationSidebar";
import { MessageList } from "../../components/MessageList";
import { ModelPicker } from "../../components/ModelPicker";
import { NewConversationDialog } from "../../components/NewConversationDialog";
import {
  useArchiveConversation,
  useConversationMessages,
  useConversations,
  useCreateConversation,
  useSelectConversationModel
} from "../../hooks/useChat";
import { useChatStream } from "../../hooks/useChatStream";
import { useChatDraftStore } from "../../stores/useChatDraftStore";
import type { Conversation, ConversationMode } from "../../types/chatTypes";
import type { ChatDraftState } from "../../types/chatDraftTypes";
import {
  Chat,
  ChatContent,
  ConversationBody,
  ConversationColumn,
  Header,
  HeaderCopy,
  HeaderMeta,
  HeaderTitle,
  Layout,
  LoadFailure,
  ModelArea,
  PrivacyBadge,
  WorkspaceContext
} from "./styles";

export function ChatPage(): ReactElement {
  const navigate: NavigateFunction = useNavigate();
  const conversations = useConversations();
  const create = useCreateConversation();
  const archive = useArchiveConversation();
  const providers = useProviderRegistry();
  const activeWorkspace = useActiveWorkspace();
  const workspaceCheck: WorkspaceCheck = useWorkspaceStore((state: WorkspaceState) => state.workspaceCheck);
  const workspaceHydration: WorkspaceState["hydrationStatus"] = useWorkspaceStore((state: WorkspaceState) => state.hydrationStatus);
  const workspaceInspection = useWorkspaceCheck();

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [mode, setMode] = useState<ConversationMode>("chat");
  const [isContextOpen, setIsContextOpen] = useState<boolean>(false);
  const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false);
  const initialDraft: string = useChatDraftStore((state: ChatDraftState) => state.content);
  const clearDraft: ChatDraftState["clear"] = useChatDraftStore((state: ChatDraftState) => state.clear);

  const messages = useConversationMessages(selectedId);
  const selectModel = useSelectConversationModel(selectedId);
  const stream = useChatStream(selectedId);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);

  useEffect((): void => {
    if (!selectedId && conversations.data?.[0]) setSelectedId(conversations.data[0].id);
  }, [conversations.data, selectedId]);

  useEffect((): void => {
    if (initialDraft) clearDraft();
  }, [clearDraft, initialDraft]);

  useEffect((): void => {
    if (
      workspaceHydration === "ready"
      && activeWorkspace
      && workspaceCheck.status === "idle"
    ) {
      workspaceInspection.checkActiveWorkspace(false);
    }
  }, [activeWorkspace, workspaceCheck.status, workspaceHydration, workspaceInspection]);

  const selected: Conversation | undefined = conversations.data
    ?.find((item: Conversation) => item.id === selectedId);
  const configuredProviders: ProviderConfiguration[] = providers.registry.data
    ?.filter((provider: ProviderConfiguration) => Boolean(provider.selectedModel)) ?? [];
  const hasModel: boolean = Boolean(selected?.selectedModel);
  const hasConfiguredProvider: boolean = configuredProviders.length > 0;

  const createConversation = (title: string): void => {
    create.mutate(title, {
      onSuccess: (conversation: Conversation): void => {
        setSelectedId(conversation.id);
        setIsDialogOpen(false);
      }
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
    <Layout>
      <ConversationSidebar
        conversations={conversations.data ?? []}
        selectedId={selectedId}
        isCreating={create.isPending}
        onSelect={setSelectedId}
        onNew={(): void => setIsDialogOpen(true)}
        onArchive={archiveConversation}
      />

      <Chat>
        <Header>
          <HeaderCopy>
            <HeaderTitle>{selected?.title ?? "Start a conversation"}</HeaderTitle>
            <HeaderMeta>
              <PrivacyBadge title="Private to your account"><LockKey size={12} weight="bold" /> Private</PrivacyBadge>
              <WorkspaceContext
                type="button"
                $active={Boolean(activeWorkspace)}
                title={activeWorkspace ? `Local workspace: ${activeWorkspace.directoryName}` : "Choose a workspace"}
                onClick={(): void => { navigate("/projects"); }}
              >
                <FolderOpen size={15} weight={activeWorkspace ? "fill" : "duotone"} />
                Workspace: {activeWorkspace?.name ?? "Choose workspace"}
              </WorkspaceContext>
              {selected?.selectedModel && <span><Cpu size={12} /> Local</span>}
            </HeaderMeta>
          </HeaderCopy>
          <ModelArea>
            <ModelPicker
              providers={configuredProviders}
              selectedProviderId={selected?.providerConfigurationId ?? null}
              disabled={!selectedId || selectModel.isPending || stream.isBusy}
              onSelect={(providerConfigurationId: string, selectedModel: string): void =>
                selectModel.mutate({ providerConfigurationId, selectedModel })}
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
                streamingContent={stream.streamingContent}
                errorMessage={stream.errorMessage}
                mode={mode}
                onConfigureProvider={(): void => { navigate("/settings/providers"); }}
              />

              <ChatComposer
                initialContent={initialDraft}
                disabled={!selectedId}
                hasModel={hasModel}
                phase={stream.phase}
                isBusy={stream.isBusy}
                mode={mode}
                onModeChange={setMode}
                onSend={stream.send}
                onCancel={stream.cancel}
              />
            </ConversationColumn>
            <ConversationContextPanel mode={mode} open={isContextOpen} onOpenChange={setIsContextOpen} />
          </ConversationBody>
        </ChatContent>
      </Chat>

      <NewConversationDialog
        open={isDialogOpen}
        isSaving={create.isPending}
        onOpenChange={setIsDialogOpen}
        onCreate={createConversation}
      />
    </Layout>
  );
}
