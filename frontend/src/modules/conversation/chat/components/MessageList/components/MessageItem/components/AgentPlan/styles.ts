import styled from "styled-components";
import type { AgentPlanStepStatus } from "../../../../../../types/chatTypes";

export const PlanSurface = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.xs};
  margin: 0 0 ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
`;

export const PlanHead = styled.div`
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.68rem;
  font-weight: 750;

  small {
    margin-left: auto;
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.56rem;
    font-weight: 600;
  }
`;

export const PlanExplanation = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.64rem;
  line-height: 1.45;
`;

export const PlanSteps = styled.ol`
  display: grid;
  gap: 0.32rem;
  max-height: 13rem;
  overflow-y: auto;
  margin: 0;
  padding: 0;
  list-style: none;
`;

export const PlanStep = styled.li<{ $status: AgentPlanStepStatus }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: 0.4rem;
  color: ${({ theme, $status }) => $status === "COMPLETED"
    ? theme.colors.textSubtle
    : theme.colors.text};
  font-size: 0.64rem;
  line-height: 1.4;

  svg {
    margin-top: 0.08rem;
    color: ${({ theme, $status }) => $status === "IN_PROGRESS"
      ? theme.colors.primary
      : $status === "COMPLETED" ? theme.colors.primarySoft : theme.colors.textSubtle};
  }

  .plan-spinner { animation: plan-spin 1s linear infinite; }

  @keyframes plan-spin {
    to { transform: rotate(360deg); }
  }
`;
