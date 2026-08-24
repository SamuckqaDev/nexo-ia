import {
  BookOpen,
  CheckCircle,
  PlugsConnected,
  Robot,
  WarningCircle
} from "@phosphor-icons/react";
import type { ReactElement } from "react";
import type { AgentContextSummary } from "../../../../types/chatViewTypes";
import {
  ContextAction,
  ContextCopy,
  ContextGrid,
  ContextHeading,
  ContextPanel,
  ContextStatus
} from "./styles";

type AgentContextIndicatorProps = {
  context: AgentContextSummary;
  onInspectKnowledge: () => void;
  onManageMcp: () => void;
};

export function AgentContextIndicator({
  context,
  onInspectKnowledge,
  onManageMcp
}: AgentContextIndicatorProps): ReactElement {
  const knowledgeReady: boolean = context.selectedVaultNames.length > 0;
  const mcpReady: boolean = context.enabledMcpToolCount > 0;
  const modelBlocked: boolean = context.modelToolCallingSupported === false;
  const knowledgeDetail: string = context.knowledgeLoading
    ? "Loading Vaults…"
    : context.knowledgeError
      ? "Knowledge status unavailable"
      : knowledgeReady
        ? context.selectedVaultNames.join(", ")
        : "No Vault selected";
  const mcpDetail: string = context.mcpLoading
    ? "Loading MCP tools…"
    : context.mcpError
      ? "MCP status unavailable"
      : mcpReady
        ? `${context.enabledMcpToolCount} tool${context.enabledMcpToolCount === 1 ? "" : "s"} from ${context.enabledMcpConnectionNames.join(", ")}`
        : "No MCP connected — open Hub";

  return (
    <ContextPanel aria-label="Agent context" $blocked={modelBlocked}>
      <ContextHeading>
        <Robot size={15} weight="duotone" />
        <strong>Agent context</strong>
        <ContextStatus $ready={!modelBlocked}>
          {modelBlocked ? <WarningCircle size={12} /> : <CheckCircle size={12} weight="fill" />}
          {modelBlocked
            ? "Selected model has no tool calling"
            : context.modelToolCallingSupported === null ? "Tool support unknown" : "Agent ready"}
        </ContextStatus>
      </ContextHeading>
      <ContextGrid>
        <ContextAction type="button" $ready={knowledgeReady} onClick={onInspectKnowledge}>
          <BookOpen size={16} weight={knowledgeReady ? "fill" : "duotone"} />
          <ContextCopy><strong>Knowledge</strong><span title={knowledgeDetail}>{knowledgeDetail}</span></ContextCopy>
        </ContextAction>
        <ContextAction type="button" $ready={mcpReady} onClick={onManageMcp}>
          <PlugsConnected size={16} weight={mcpReady ? "fill" : "duotone"} />
          <ContextCopy><strong>MCP tools</strong><span title={mcpDetail}>{mcpDetail}</span></ContextCopy>
        </ContextAction>
      </ContextGrid>
    </ContextPanel>
  );
}
