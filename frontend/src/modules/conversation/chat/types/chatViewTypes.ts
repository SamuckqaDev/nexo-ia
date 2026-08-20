import type { SkillDefinition } from "../../../skill/catalog/types/skillTypes";
import type { VaultSourceReference } from "../../../knowledge/vault/types/vaultTypes";
import type { ConversationMode, StreamPhase } from "./chatTypes";

export type ConversationContextSection = "workspace" | "vaults" | "plan" | "artifacts" | "media" | "tasks";

export type ConversationContextPanelProps = {
  conversationId: string | null;
  mode: ConversationMode;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onManageVaults: () => void;
  onManageWorkspace: () => void;
};

export type ChatComposerProps = {
  initialContent?: string;
  disabled: boolean;
  hasModel: boolean;
  phase: StreamPhase;
  isBusy: boolean;
  mode: ConversationMode;
  onModeChange: (mode: ConversationMode) => void;
  onSend: (content: string) => void;
  onCancel: () => void;
};

export type ContextualChatMessage = {
  content: string;
  skillName: string | null;
  vaultSourceNames: string[];
};

export type ExplicitChatContext = {
  skill: SkillDefinition | null;
  vaultSources: VaultSourceReference[];
};

export type ChatLoadingProps = {
  title: string;
  label: string;
};
