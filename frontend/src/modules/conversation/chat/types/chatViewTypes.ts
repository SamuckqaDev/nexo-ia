import type { SkillDefinition } from "../../../skill/catalog/types/skillTypes";
import type { BackendVault } from "../../../knowledge/vault/types/backendVaultTypes";
import type { VaultSourceReference } from "../../../knowledge/vault/types/vaultTypes";
import type { AgentPlan, AgentState, ConversationMode, StreamPhase, ToolExecution } from "./chatTypes";

export type ConversationContextSection = "workspace" | "vaults" | "memory" | "plan" | "artifacts" | "media" | "tasks";

export type ConversationContextPanelProps = {
  conversationId: string | null;
  mode: ConversationMode;
  agentPlan: AgentPlan | null;
  agentState: AgentState | null;
  toolExecutions: ToolExecution[];
  open: boolean;
  vaults: BackendVault[];
  selectedVaultIds: string[];
  isVaultSelectionPending: boolean;
  vaultSelectionError: string | null;
  onOpenChange: (open: boolean) => void;
  onToggleVault: (vaultId: string) => void;
  onManageVaults: () => void;
  onManageWorkspace: () => void;
};

export type AgentContextSummary = {
  selectedVaultNames: string[];
  enabledMcpConnectionNames: string[];
  enabledMcpToolCount: number;
  knowledgeLoading: boolean;
  knowledgeError: boolean;
  mcpLoading: boolean;
  mcpError: boolean;
  modelToolCallingSupported: boolean | null;
  modelThinkingSupported: boolean | null;
  thinkingEnabled: boolean;
};

export type ChatComposerProps = {
  initialContent?: string;
  messageHistory?: string[];
  disabled: boolean;
  hasModel: boolean;
  phase: StreamPhase;
  isBusy: boolean;
  mode: ConversationMode;
  agentContext: AgentContextSummary;
  onModeChange: (mode: ConversationMode) => void;
  onInspectKnowledge: () => void;
  onManageMcp: () => void;
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
  workspace?: { id: string; name: string } | null;
};

export type ChatLoadingProps = {
  title: string;
  label: string;
};
