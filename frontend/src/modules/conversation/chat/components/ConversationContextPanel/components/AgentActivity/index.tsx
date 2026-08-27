import {
  BookOpen,
  CheckCircle,
  ClipboardText,
  Info,
  SpinnerGap,
  WarningCircle
} from "@phosphor-icons/react";
import type { ReactElement } from "react";
import type { AgentPlan, AgentState, ToolExecution } from "../../../../types/chatTypes";
import {
  ActivityCopy,
  ActivityEvidence,
  ActivityFeed,
  ActivityHead,
  ActivityMarker,
  ActivityRow,
  ActivityState,
  ActivitySummary
} from "./styles";

type AgentActivityProps = {
  plan: AgentPlan | null;
  state: AgentState | null;
  executions: ToolExecution[];
};

const toolLabel = (toolName: string): string => {
  if (toolName === "update_plan") return "Revised the implementation plan";
  if (toolName === "inspect_capabilities") return "Inspected authorized capabilities";
  if (toolName === "search_knowledge") return "Searched selected Knowledge Vaults";
  if (toolName === "save_to_vault") return "Saved knowledge to the selected Vault";
  if (toolName === "remember") return "Updated personal memory";
  if (toolName === "workspace_list_files") return "Listed workspace files";
  if (toolName === "workspace_read_file") return "Read a workspace file";
  if (toolName === "workspace_search") return "Searched workspace source";
  if (toolName === "workspace_git_status") return "Inspected workspace Git status";
  if (toolName === "workspace_git_diff") return "Read a workspace Git diff";
  if (toolName === "workspace_inspect_project") return "Inspected project stack";
  if (toolName === "workspace_apply_patch") return "Prepared a server-side edit preview";
  if (toolName === "workspace_create_file") return "Prepared a server-side file preview";
  if (toolName === "workspace_delete_file") return "Prepared a server-side deletion preview";
  if (toolName.startsWith("mcp_")) {
    return `Called MCP · ${toolName.replace(/^mcp_[a-f0-9]{8}_/, "").replaceAll("_", " ")}`;
  }
  return `Called ${toolName}`;
};

const stateCopy = (state: AgentState | null): { title: string; description: string } => {
  if (state === "PLANNING") return {
    title: "Preparing the implementation plan",
    description: "Nexo is dividing the request into observable actions before execution."
  };
  if (state === "RUNNING") return {
    title: "Executing authorized actions",
    description: "New tool calls appear below as soon as the runtime starts them."
  };
  if (state === "VERIFYING") return {
    title: "Verifying evidence and result",
    description: "Nexo is checking completed actions before finishing the answer."
  };
  if (state === "COMPLETED") return {
    title: "Agent run completed",
    description: "The final answer and every recorded action remain attached to this conversation."
  };
  if (state === "FAILED") return {
    title: "Agent run failed",
    description: "Review the failed or unavailable action below before trying again."
  };
  if (state === "CANCELLED") return {
    title: "Agent run cancelled",
    description: "The user stopped this run; completed actions remain visible."
  };
  if (state === "BLOCKED") return {
    title: "Agent run blocked",
    description: "Nexo needs a missing capability or user decision before it can continue."
  };
  return {
    title: "Waiting for an Agent run",
    description: "Plans and confirmed runtime actions will appear here."
  };
};

const executionCopy = (execution: ToolExecution): string => {
  if (execution.status === "RUNNING") return "The runtime started this action and it is still running.";
  if (execution.status === "FOUND") {
    if (execution.toolName.startsWith("workspace_")) {
      return "The workspace tool completed and returned matching project evidence.";
    }
    return `Knowledge search completed with ${execution.citations.length} cited source${execution.citations.length === 1 ? "" : "s"}.`;
  }
  if (execution.status === "NO_RESULTS") return "The tool completed and returned no matching result.";
  if (execution.status === "COMPLETED" && [
    "workspace_apply_patch",
    "workspace_create_file",
    "workspace_delete_file"
  ].includes(execution.toolName)) {
    return "The server generated an exact preview. The file remains unchanged until approval in Artifacts.";
  }
  if (execution.status === "COMPLETED") return "The runtime confirmed that this action completed.";
  if (execution.status === "DENIED") return "The action was denied by the request policy.";
  if (execution.status === "UNAVAILABLE") return "The required runtime or connection was unavailable.";
  return "The action failed safely without being reported as completed.";
};

const time = (value: string): string => new Intl.DateTimeFormat(undefined, {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit"
}).format(new Date(value));

export function AgentActivity({ plan, state, executions }: AgentActivityProps): ReactElement {
  const copy = stateCopy(state);
  const sortedExecutions: ToolExecution[] = [...executions]
    .sort((left: ToolExecution, right: ToolExecution): number =>
      new Date(left.startedAt).getTime() - new Date(right.startedAt).getTime());

  return (
    <ActivityFeed aria-label="Agent tasks" aria-live="polite">
      <ActivitySummary>
        {state === "PLANNING" || state === "RUNNING" || state === "VERIFYING"
          ? <SpinnerGap className="activity-spinner" size={15} weight="bold" />
          : state === "FAILED" || state === "BLOCKED"
            ? <WarningCircle size={15} weight="fill" />
            : <Info size={15} weight="duotone" />}
        <div><strong>{copy.title}</strong><span>{copy.description}</span></div>
        {state && <ActivityState>{state.toLowerCase()}</ActivityState>}
      </ActivitySummary>

      {plan && (
        <ActivityRow $tone="success">
          <ActivityMarker><ClipboardText size={14} weight="duotone" /><i aria-hidden /></ActivityMarker>
          <ActivityCopy>
            <ActivityHead><strong>Published implementation plan</strong><time>{time(plan.updatedAt)}</time></ActivityHead>
            <span>Revision {plan.revision} contains {plan.steps.length} observable step{plan.steps.length === 1 ? "" : "s"}.</span>
          </ActivityCopy>
        </ActivityRow>
      )}

      {sortedExecutions.map((execution: ToolExecution) => {
        const failed: boolean = execution.status === "FAILED"
          || execution.status === "DENIED"
          || execution.status === "UNAVAILABLE";
        return (
          <ActivityRow key={execution.id} $tone={execution.status === "RUNNING" ? "running" : failed ? "danger" : "success"}>
            <ActivityMarker>
              {execution.status === "RUNNING"
                ? <SpinnerGap className="activity-spinner" size={14} weight="bold" />
                : failed
                  ? <WarningCircle size={14} weight="fill" />
                  : <CheckCircle size={14} weight="fill" />}
              <i aria-hidden />
            </ActivityMarker>
            <ActivityCopy>
              <ActivityHead>
                <strong>{toolLabel(execution.toolName)}</strong>
                <time>{time(execution.startedAt)}</time>
              </ActivityHead>
              <span>{executionCopy(execution)}</span>
              <small>{execution.status.toLowerCase().replaceAll("_", " ")}{execution.durationMs !== null
                ? ` · ${(execution.durationMs / 1000).toFixed(1)}s`
                : ""}</small>
              {execution.citations.length > 0 && (
                <ActivityEvidence>
                  {execution.citations.map((citation, index: number) => (
                    <span key={`${citation.vaultName}-${citation.sourceDisplayName}-${citation.chunkOrdinal}-${index}`} title={citation.excerpt}>
                      <BookOpen size={11} weight="duotone" />
                      {citation.vaultName}/{citation.sourceDisplayName}#{citation.chunkOrdinal}
                    </span>
                  ))}
                </ActivityEvidence>
              )}
            </ActivityCopy>
          </ActivityRow>
        );
      })}
    </ActivityFeed>
  );
}
