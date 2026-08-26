import { CheckCircle, Circle, ListChecks, SpinnerGap } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import type { AgentPlan as AgentPlanValue, AgentPlanStepStatus } from "../../types/chatTypes";
import {
  PlanExplanation,
  PlanHead,
  PlanProgress,
  PlanStep,
  PlanStepCopy,
  PlanStepMarker,
  PlanSteps,
  PlanSurface
} from "./styles";

type AgentPlanProps = {
  plan: AgentPlanValue;
};

const StepIcon = ({ status }: { status: AgentPlanStepStatus }): ReactElement => {
  if (status === "COMPLETED") return <CheckCircle size={13} weight="fill" aria-hidden />;
  if (status === "IN_PROGRESS") return <SpinnerGap className="plan-spinner" size={13} weight="bold" aria-hidden />;
  return <Circle size={13} aria-hidden />;
};

/** Reusable implementation-plan surface for both the message timeline and conversation workspace. */
export function AgentPlan({ plan }: AgentPlanProps): ReactElement {
  const completed: number = plan.steps.filter((step) => step.status === "COMPLETED").length;

  return (
    <PlanSurface aria-label="Agent implementation plan">
      <PlanHead>
        <ListChecks size={14} weight="duotone" aria-hidden />
        <span>Implementation plan</span>
        <small>revision {plan.revision}</small>
      </PlanHead>
      {plan.explanation && <PlanExplanation>{plan.explanation}</PlanExplanation>}
      <PlanProgress>
        <span>{completed} of {plan.steps.length} completed</span>
        <progress max={Math.max(plan.steps.length, 1)} value={completed} aria-label="Implementation plan progress" />
      </PlanProgress>
      <PlanSteps>
        {plan.steps.map((item, index: number) => (
          <PlanStep key={`${index}-${item.step}`} $status={item.status}>
            <PlanStepMarker $status={item.status}>
              <StepIcon status={item.status} />
              {index < plan.steps.length - 1 && <i aria-hidden />}
            </PlanStepMarker>
            <PlanStepCopy><small>{index + 1}</small>{item.step}</PlanStepCopy>
          </PlanStep>
        ))}
      </PlanSteps>
    </PlanSurface>
  );
}
