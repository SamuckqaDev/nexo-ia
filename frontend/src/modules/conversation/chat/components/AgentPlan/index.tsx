import { CheckCircle, Circle, ListChecks, SpinnerGap } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import type { AgentPlan as AgentPlanValue, AgentPlanStepStatus } from "../../types/chatTypes";
import { PlanExplanation, PlanHead, PlanStep, PlanSteps, PlanSurface } from "./styles";

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
  return (
    <PlanSurface aria-label="Agent implementation plan">
      <PlanHead>
        <ListChecks size={14} weight="duotone" aria-hidden />
        Implementation plan
        <small>revision {plan.revision}</small>
      </PlanHead>
      {plan.explanation && <PlanExplanation>{plan.explanation}</PlanExplanation>}
      <PlanSteps>
        {plan.steps.map((item, index: number) => (
          <PlanStep key={`${index}-${item.step}`} $status={item.status}>
            <StepIcon status={item.status} />
            <span>{item.step}</span>
          </PlanStep>
        ))}
      </PlanSteps>
    </PlanSurface>
  );
}
