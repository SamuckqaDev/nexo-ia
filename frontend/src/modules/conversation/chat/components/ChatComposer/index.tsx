import { ArrowUp, ChatCircleDots, ImageSquare, PaperPlaneRight, PlugsConnected, Robot, Sparkle, Stop, X } from "@phosphor-icons/react";
import { useMemo, useRef, useState, type ChangeEvent, type FormEvent, type KeyboardEvent, type ReactElement } from "react";
import { useSkillCatalogStore } from "../../../../skill/catalog/stores/useSkillCatalogStore";
import type { SkillCatalogState, SkillDefinition } from "../../../../skill/catalog/types/skillTypes";
import { useActiveWorkspace } from "../../../../project/workspace/hooks/useActiveWorkspace";
import { buildContextualChatMessage } from "../../services/chatContextService";
import type { ChatComposerProps } from "../../types/chatViewTypes";
import { AgentContextIndicator } from "./components/AgentContextIndicator";
import {
  ActiveContext,
  ActiveContextCopy,
  CapabilityButton,
  Composer,
  ComposerActions,
  ComposerCard,
  ComposerFooter,
  ComposerSurface,
  Field,
  Hint,
  HistoryCopy,
  HistoryMenu,
  HistoryOption,
  ModeButton,
  ModeControl,
  RemoveContext,
  SendButton,
  SkillCommand,
  SkillCopy,
  SkillMenu,
  SkillOption,
  SkillPaletteHint,
  StopButton
} from "./styles";

export function ChatComposer({
  initialContent = "",
  messageHistory = [],
  disabled,
  hasModel,
  phase,
  isBusy,
  mode,
  agentContext,
  onModeChange,
  onInspectKnowledge,
  onManageMcp,
  onSend,
  onCancel
}: ChatComposerProps): ReactElement {
  const [content, setContent] = useState<string>(initialContent);
  const [activeSkill, setActiveSkill] = useState<SkillDefinition | null>(null);
  const [skillIndex, setSkillIndex] = useState<number>(0);
  const [skillMenuDismissed, setSkillMenuDismissed] = useState<boolean>(false);
  const [historyOpen, setHistoryOpen] = useState<boolean>(false);
  const [historyIndex, setHistoryIndex] = useState<number>(-1);
  const field = useRef<HTMLTextAreaElement>(null);
  const draftBeforeHistory = useRef<string>("");
  const skills: SkillDefinition[] = useSkillCatalogStore((state: SkillCatalogState) => state.skills);
  const activeWorkspace = useActiveWorkspace();
  const recentMessages: string[] = useMemo<string[]>(() => {
    const seen = new Set<string>();
    const recent: string[] = [];
    for (let index = messageHistory.length - 1; index >= 0 && recent.length < 12; index -= 1) {
      const message: string = messageHistory[index]?.trim() ?? "";
      if (!message || seen.has(message)) continue;
      seen.add(message);
      recent.push(message);
    }
    return recent;
  }, [messageHistory]);
  const skillQuery: string = content.startsWith("/") ? content.slice(1).toLowerCase() : "";
  const skillMenuOpen: boolean = !historyOpen && !skillMenuDismissed && content.startsWith("/") && !/\s/.test(skillQuery);
  const visibleSkills: SkillDefinition[] = useMemo<SkillDefinition[]>(() => skills
    .filter((skill: SkillDefinition): boolean => skill.enabled)
    .filter((skill: SkillDefinition): boolean => skill.scope !== "project" || skill.scopeTarget === activeWorkspace?.id)
    .filter((skill: SkillDefinition): boolean =>
      !skillQuery || `${skill.command} ${skill.name} ${skill.description}`.toLowerCase().includes(skillQuery))
    .slice(0, 8), [activeWorkspace?.id, skillQuery, skills]);

  const selectSkill = (skill: SkillDefinition): void => {
    setActiveSkill(skill);
    setContent("");
    setSkillMenuDismissed(true);
    setSkillIndex(0);
    field.current?.focus();
  };

  const send = (): void => {
    if (!content.trim() || isBusy || (mode === "agent" && agentContext.modelToolCallingSupported === false)) return;
    onSend(buildContextualChatMessage(content, {
      skill: activeSkill,
      vaultSources: [],
      workspace: activeWorkspace ? { id: activeWorkspace.id, name: activeWorkspace.name } : null
    }));
    setContent("");
    setActiveSkill(null);
    setHistoryOpen(false);
    setHistoryIndex(-1);
  };

  const selectHistoryMessage = (message: string): void => {
    setContent(message);
    setHistoryOpen(false);
    setHistoryIndex(-1);
    setSkillMenuDismissed(true);
    field.current?.focus();
  };

  const openHistory = (): void => {
    if (!recentMessages.length) return;
    draftBeforeHistory.current = content;
    setHistoryOpen((current: boolean): boolean => !current);
    setHistoryIndex(-1);
    setSkillMenuDismissed(true);
    field.current?.focus();
  };

  const submit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    send();
  };

  const submitOnEnter = (event: KeyboardEvent<HTMLTextAreaElement>): void => {
    if (skillMenuOpen) {
      if (event.key === "ArrowDown" || event.key === "ArrowUp") {
        event.preventDefault();
        const direction: number = event.key === "ArrowDown" ? 1 : -1;
        setSkillIndex((current: number): number => visibleSkills.length
          ? (current + direction + visibleSkills.length) % visibleSkills.length
          : 0);
        return;
      }
      if ((event.key === "Enter" || event.key === "Tab") && visibleSkills[skillIndex]) {
        event.preventDefault();
        selectSkill(visibleSkills[skillIndex]);
        return;
      }
      if (event.key === "Escape") {
        event.preventDefault();
        setSkillMenuDismissed(true);
        return;
      }
    }
    if (event.key === "ArrowUp" && recentMessages.length && (!content || historyOpen)) {
      event.preventDefault();
      const nextIndex: number = historyOpen
        ? Math.min(historyIndex + 1, recentMessages.length - 1)
        : 0;
      if (!historyOpen) draftBeforeHistory.current = content;
      setHistoryOpen(true);
      setHistoryIndex(nextIndex);
      setContent(recentMessages[nextIndex]);
      setSkillMenuDismissed(true);
      return;
    }
    if (event.key === "ArrowDown" && historyOpen) {
      event.preventDefault();
      if (historyIndex <= 0) {
        setContent(draftBeforeHistory.current);
        setHistoryOpen(false);
        setHistoryIndex(-1);
        return;
      }
      const nextIndex: number = historyIndex - 1;
      setHistoryIndex(nextIndex);
      setContent(recentMessages[nextIndex]);
      return;
    }
    if (event.key === "Escape" && historyOpen) {
      event.preventDefault();
      setContent(draftBeforeHistory.current);
      setHistoryOpen(false);
      setHistoryIndex(-1);
      return;
    }
    if (event.key !== "Enter" || event.shiftKey || isBusy || !content.trim()) return;

    event.preventDefault();
    send();
  };

  const updateContent = (event: ChangeEvent<HTMLTextAreaElement>): void => {
    setContent(event.target.value);
    setHistoryOpen(false);
    setHistoryIndex(-1);
    setSkillMenuDismissed(false);
    setSkillIndex(0);
  };

  const openSkills = (): void => {
    setContent("/");
    setHistoryOpen(false);
    setHistoryIndex(-1);
    setSkillMenuDismissed(false);
    setSkillIndex(0);
    field.current?.focus();
  };

  return (
    <ComposerCard>
      <ComposerSurface>
        {!hasModel && !disabled && <Hint>Select a model to enable this conversation.</Hint>}
        {phase === "cancelling" && <Hint>Stopping the answer…</Hint>}
        <Composer onSubmit={submit}>
          {mode === "agent" && (
            <AgentContextIndicator
              context={agentContext}
              onInspectKnowledge={onInspectKnowledge}
              onManageMcp={onManageMcp}
            />
          )}
          {activeSkill && (
            <ActiveContext>
              <Sparkle size={17} weight="fill" />
              <ActiveContextCopy><strong>Skill: /{activeSkill.command}</strong><span>Skill instructions will be included in this message. Dependencies remain permission-gated.</span></ActiveContextCopy>
              <RemoveContext type="button" aria-label={`Remove ${activeSkill.name} Skill`} onClick={(): void => setActiveSkill(null)}><X size={14} /></RemoveContext>
            </ActiveContext>
          )}
          <Field
            ref={field}
            aria-label="Message"
            placeholder={hasModel
              ? mode === "agent" ? "Describe an objective for Nexo Agent…" : "Message Nexo IA…"
              : "Type your first message…"}
            value={content}
            maxLength={8000}
            disabled={disabled || isBusy}
            onChange={updateContent}
            onKeyDown={submitOnEnter}
          />

          <ComposerFooter>
            <ModeControl aria-label="Conversation mode">
              <ModeButton type="button" $active={mode === "chat"} onClick={(): void => onModeChange("chat")}>
                <ChatCircleDots size={15} weight="duotone" /> Chat
              </ModeButton>
              <ModeButton type="button" $active={mode === "agent"} $agent onClick={(): void => onModeChange("agent")}>
                <Robot size={15} weight="duotone" /> Agent
              </ModeButton>
            </ModeControl>
            <CapabilityButton type="button" title="Open your MCP tools" onClick={onManageMcp}>
              <PlugsConnected size={16} weight="duotone" />
              <span>{mode === "agent" ? `MCP ${agentContext.enabledMcpToolCount}` : "MCP"}</span>
            </CapabilityButton>
            <CapabilityButton type="button" title="Choose a Skill or type /" onClick={openSkills}>
              <Sparkle size={16} weight="duotone" /> <span>Skills</span>
            </CapabilityButton>
            <CapabilityButton type="button" disabled title="Image generation requires the ComfyUI runtime">
              <ImageSquare size={16} weight="duotone" /> <span>Image</span>
            </CapabilityButton>
            <ComposerActions>
              {isBusy ? (
                <StopButton type="button" aria-label="Stop response" disabled={phase === "starting" || phase === "cancelling"} onClick={onCancel}>
                  <Stop size={18} weight="fill" />
                </StopButton>
              ) : (
                <SendButton
                  type="submit"
                  aria-label="Send message"
                  disabled={disabled || !hasModel || !content.trim()
                    || (mode === "agent" && agentContext.modelToolCallingSupported === false)}
                >
                  <PaperPlaneRight size={19} weight="fill" />
                </SendButton>
              )}
            </ComposerActions>
          </ComposerFooter>
        </Composer>
      </ComposerSurface>
      {skillMenuOpen && (
        <SkillMenu role="listbox" aria-label="Available Skills">
          <SkillPaletteHint><span>Skills</span><small>Type to filter · Enter to select · Esc to close</small></SkillPaletteHint>
          {visibleSkills.length ? visibleSkills.map((skill: SkillDefinition, index: number) => (
            <SkillOption
              key={skill.id}
              type="button"
              role="option"
              aria-selected={index === skillIndex}
              $active={index === skillIndex}
              onMouseEnter={(): void => setSkillIndex(index)}
              onClick={(): void => selectSkill(skill)}
            >
              <Sparkle size={17} weight="duotone" />
              <SkillCopy><strong>{skill.name}</strong><span>{skill.description}</span></SkillCopy>
              <SkillCommand>/{skill.command}</SkillCommand>
            </SkillOption>
          )) : <SkillPaletteHint><span>No matching Skill</span><small>Open Skills from the sidebar to create one.</small></SkillPaletteHint>}
        </SkillMenu>
      )}
      {historyOpen && (
        <HistoryMenu role="listbox" aria-label="Recent messages">
          <SkillPaletteHint><span>Recent messages</span><small>↑/↓ to browse · Enter to send · Esc to restore</small></SkillPaletteHint>
          {recentMessages.map((message: string, index: number) => (
            <HistoryOption
              key={`${index}-${message}`}
              type="button"
              role="option"
              aria-selected={index === historyIndex}
              $active={index === historyIndex}
              onClick={(): void => selectHistoryMessage(message)}
            >
              <ArrowUp size={15} weight="bold" />
              <HistoryCopy>{message}</HistoryCopy>
            </HistoryOption>
          ))}
        </HistoryMenu>
      )}
    </ComposerCard>
  );
}
