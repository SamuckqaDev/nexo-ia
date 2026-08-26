import { ImageSquare, Robot } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { ImageGenerationProgress } from "../../../../../media/components/ImageGenerationProgress";
import type { ImageGenerationJob } from "../../../../../media/types/imageGenerationTypes";
import type { AgentPlan, AgentState, ToolExecution } from "../../../../types/chatTypes";
import { AgentActivity } from "../AgentActivity";
import { TaskGroup, TaskGroupHeader, TaskStack } from "./styles";

type ConversationTasksProps = {
  plan: AgentPlan | null;
  state: AgentState | null;
  executions: ToolExecution[];
  imageJobs: ImageGenerationJob[];
};

export function ConversationTasks({
  plan,
  state,
  executions,
  imageJobs
}: ConversationTasksProps): ReactElement {
  const activeImageJobs: number = imageJobs.filter((job: ImageGenerationJob): boolean =>
    job.status === "QUEUED" || job.status === "GENERATING").length;
  const hasAgentRun: boolean = Boolean(state || plan || executions.length);

  return (
    <TaskStack aria-label="Conversation tasks" aria-live="polite">
      {imageJobs.length > 0 && (
        <TaskGroup>
          <TaskGroupHeader>
            <span><ImageSquare size={15} weight="duotone" />Image generation</span>
            <small>{activeImageJobs > 0
              ? `${activeImageJobs} running`
              : `${imageJobs.length} finished without media`}</small>
          </TaskGroupHeader>
          {imageJobs.map((job: ImageGenerationJob) => (
            <ImageGenerationProgress key={job.id} job={job} />
          ))}
        </TaskGroup>
      )}
      {hasAgentRun && (
        <TaskGroup>
          <TaskGroupHeader>
            <span><Robot size={15} weight="duotone" />Agent tasks</span>
            <small>{executions.length} action{executions.length === 1 ? "" : "s"}</small>
          </TaskGroupHeader>
          <AgentActivity plan={plan} state={state} executions={executions} />
        </TaskGroup>
      )}
    </TaskStack>
  );
}
