import { useEffect, useState, type FormEvent, type ReactElement } from "react";
import { useProviderRegistry } from "../../../../provider/hooks/useProviderRegistry";
import { useConversationMessages, useConversations, useCreateConversation, useSelectConversationModel, useSendMessage } from "../../hooks/useChat";
import type { ConversationMode } from "../../types/chatTypes";
import { Bubble, CapabilityNote, Chat, ChatHeader, Composer, ConversationButton, ConversationList, Empty, HeaderControls, Input, Layout, ModeButton, ModelSelect, NewChat, Messages, Send, Sidebar } from "./styles";

export function ChatPage(): ReactElement {
  const conversations = useConversations();
  const create = useCreateConversation();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [content, setContent] = useState<string>("");
  const [mode, setMode] = useState<ConversationMode>("chat");
  const messages = useConversationMessages(selectedId);
  const send = useSendMessage(selectedId);
  const selectModel = useSelectConversationModel(selectedId);
  const providers = useProviderRegistry();
  useEffect(() => { if (!selectedId && conversations.data?.[0]) setSelectedId(conversations.data[0].id); }, [conversations.data, selectedId]);
  const newConversation = (): void => { const title = window.prompt("Name this chat", "New chat"); if (title?.trim()) create.mutate(title.trim(), { onSuccess: (conversation) => setSelectedId(conversation.id) }); };
  const submit = (event: FormEvent<HTMLFormElement>): void => { event.preventDefault(); if (selectedId && content.trim() && !send.isPending) send.mutate(content.trim(), { onSuccess: () => setContent("") }); };
  const selected = conversations.data?.find((item) => item.id === selectedId);
  const configuredModels = providers.registry.data?.filter((provider) => Boolean(provider.selectedModel)) ?? [];
  const changeModel = (value: string): void => { const provider = configuredModels.find((item) => item.id === value); if (provider) selectModel.mutate({ providerConfigurationId: provider.id, selectedModel: provider.selectedModel ?? "" }); };
  return <Layout><Sidebar><NewChat type="button" onClick={newConversation} disabled={create.isPending}>+ New chat</NewChat><ConversationList>{conversations.data?.map((conversation) => <ConversationButton key={conversation.id} type="button" $active={conversation.id === selectedId} onClick={() => setSelectedId(conversation.id)}>{conversation.title}</ConversationButton>)}</ConversationList></Sidebar><Chat><ChatHeader><span>{selected?.title ?? "Chat"}</span><HeaderControls><ModeButton type="button" $active={mode === "chat"} onClick={() => setMode("chat")}>Chat</ModeButton><ModeButton type="button" $active={mode === "agent"} onClick={() => setMode("agent")}>Agent</ModeButton><ModelSelect aria-label="Model for this conversation" value={selected?.providerConfigurationId ?? ""} disabled={!selectedId || !configuredModels.length || selectModel.isPending} onChange={(event) => changeModel(event.target.value)}><option value="">{configuredModels.length ? "Select model" : "Configure a model"}</option>{configuredModels.map((provider) => <option key={provider.id} value={provider.id}>{provider.displayName} · {provider.selectedModel}</option>)}</ModelSelect></HeaderControls></ChatHeader>{mode === "agent" && <CapabilityNote>Agent mode is designed for planned, permissioned work. Its runtime is not enabled yet, so this conversation remains in safe Chat mode.</CapabilityNote>}<Messages>{!selectedId ? <Empty>Create a conversation to start.</Empty> : messages.isLoading ? <Empty>Loading messages…</Empty> : messages.data?.length ? messages.data.map((message) => <Bubble key={message.id} $user={message.role === "USER"}>{message.content}</Bubble>) : <Empty>Choose a model, then write the first message.</Empty>}</Messages><Composer onSubmit={submit}><Input aria-label="Message" placeholder={mode === "agent" ? "Describe the objective…" : "Message Nexo…"} value={content} disabled={!selectedId || !selected?.selectedModel || send.isPending} onChange={(event) => setContent(event.target.value)} /><Send type="submit" disabled={!selectedId || !selected?.selectedModel || !content.trim() || send.isPending}>{send.isPending ? "Sending…" : "Send"}</Send></Composer></Chat></Layout>;
}
