import { ChatCircleDots, ImageSquare, Paperclip, PaperPlaneRight, Robot, Sparkle, Stop, Wrench, X } from "@phosphor-icons/react";
import { useMemo, useRef, useState, type ChangeEvent, type FormEvent, type KeyboardEvent, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { attachedVaultSources, useVaultCatalogStore } from "../../../../knowledge/vault/stores/useVaultCatalogStore";
import type { VaultCatalogState, VaultSourceReference } from "../../../../knowledge/vault/types/vaultTypes";
import { useSkillCatalogStore } from "../../../../skill/catalog/stores/useSkillCatalogStore";
import type { SkillCatalogState, SkillDefinition } from "../../../../skill/catalog/types/skillTypes";
import { buildContextualChatMessage } from "../../services/chatContextService";
import type { ChatComposerProps } from "../../types/chatViewTypes";
import {
  ActiveContext,
  ActiveContextCopy,
  CapabilityButton,
  Composer,
  ComposerCard,
  ComposerFooter,
  Field,
  Hint,
  ModeButton,
  ModeControl,
  ModeHint,
  RemoveContext,
  SendButton,
  SkillCommand,
  SkillCopy,
  SkillMenu,
  SkillOption,
  SkillPaletteHint
} from "./styles";

export function ChatComposer({
  initialContent = "",
  disabled,
  hasModel,
  phase,
  isBusy,
  mode,
  onModeChange,
  onSend,
  onCancel
}: ChatComposerProps): ReactElement {
  const [content, setContent] = useState<string>(initialContent);
  const [activeSkill, setActiveSkill] = useState<SkillDefinition | null>(null);
  const [skillIndex, setSkillIndex] = useState<number>(0);
  const [skillMenuDismissed, setSkillMenuDismissed] = useState<boolean>(false);
  const field = useRef<HTMLTextAreaElement>(null);
  const skills: SkillDefinition[] = useSkillCatalogStore((state: SkillCatalogState) => state.skills);
  const vaultState: VaultCatalogState = useVaultCatalogStore();
  const vaultSources: VaultSourceReference[] = useMemo<VaultSourceReference[]>(
    () => attachedVaultSources(vaultState),
    [vaultState]
  );
  const skillQuery: string = content.startsWith("/") ? content.slice(1).toLowerCase() : "";
  const skillMenuOpen: boolean = !skillMenuDismissed && content.startsWith("/") && !/\s/.test(skillQuery);
  const visibleSkills: SkillDefinition[] = useMemo<SkillDefinition[]>(() => skills
    .filter((skill: SkillDefinition): boolean => skill.enabled)
    .filter((skill: SkillDefinition): boolean =>
      !skillQuery || `${skill.command} ${skill.name} ${skill.description}`.toLowerCase().includes(skillQuery))
    .slice(0, 8), [skillQuery, skills]);

  const selectSkill = (skill: SkillDefinition): void => {
    setActiveSkill(skill);
    setContent("");
    setSkillMenuDismissed(true);
    setSkillIndex(0);
    field.current?.focus();
  };

  const send = (): void => {
    if (!content.trim() || isBusy) return;
    onSend(buildContextualChatMessage(content, { skill: activeSkill, vaultSources }));
    setContent("");
    setActiveSkill(null);
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
    if (event.key !== "Enter" || event.shiftKey || isBusy || !content.trim()) return;

    event.preventDefault();
    send();
  };

  const updateContent = (event: ChangeEvent<HTMLTextAreaElement>): void => {
    setContent(event.target.value);
    setSkillMenuDismissed(false);
    setSkillIndex(0);
  };

  const openSkills = (): void => {
    setContent("/");
    setSkillMenuDismissed(false);
    setSkillIndex(0);
    field.current?.focus();
  };

  return (
    <ComposerCard>
      {!hasModel && !disabled && <Hint>Select a model to enable this conversation.</Hint>}
      {phase === "cancelling" && <Hint>Stopping the answer…</Hint>}
      {mode === "agent" && (
        <ModeHint>
          Agent keeps this conversation, but adds a visible plan, permissions and verified steps.
          Its execution runtime is not enabled yet.
        </ModeHint>
      )}

      <Composer onSubmit={submit}>
        {activeSkill && (
          <ActiveContext>
            <Sparkle size={17} weight="fill" />
            <ActiveContextCopy><strong>/{activeSkill.command}</strong><span>Skill instructions will be included in this message. Dependencies remain permission-gated.</span></ActiveContextCopy>
            <RemoveContext type="button" aria-label={`Remove ${activeSkill.name} Skill`} onClick={(): void => setActiveSkill(null)}><X size={14} /></RemoveContext>
          </ActiveContext>
        )}
        {vaultSources.length > 0 && (
          <ActiveContext>
            <Paperclip size={17} weight="fill" />
            <ActiveContextCopy><strong>{vaultSources.length} Vault source{vaultSources.length === 1 ? "" : "s"}</strong><span>Bounded text excerpts will be included as untrusted reference context.</span></ActiveContextCopy>
          </ActiveContext>
        )}
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
        <Field
          ref={field}
          aria-label="Message"
          placeholder={hasModel
            ? mode === "agent" ? "Describe an objective for Nexo Agent…" : "Message Nexo IA…"
            : "Choose a model first"}
          value={content}
          maxLength={8000}
          disabled={disabled || !hasModel || isBusy || mode === "agent"}
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
          <CapabilityButton type="button" disabled title="Tools will appear here when enabled">
            <Wrench size={16} weight="duotone" /> Tools
          </CapabilityButton>
          <CapabilityButton type="button" title="Choose a Skill or type /" onClick={openSkills}>
            <Sparkle size={16} weight="duotone" /> Skills
          </CapabilityButton>
          <CapabilityButton type="button" disabled title="Image generation requires the ComfyUI runtime">
            <ImageSquare size={16} weight="duotone" /> Image
          </CapabilityButton>
          {isBusy ? (
            <Button type="button" variant="outline" icon={Stop} disabled={phase === "cancelling"} onClick={onCancel}>
              Stop
            </Button>
          ) : (
            <SendButton type="submit" aria-label="Send message" disabled={disabled || !hasModel || !content.trim() || mode === "agent"}>
              <PaperPlaneRight size={19} weight="fill" />
            </SendButton>
          )}
        </ComposerFooter>
      </Composer>
    </ComposerCard>
  );
}
