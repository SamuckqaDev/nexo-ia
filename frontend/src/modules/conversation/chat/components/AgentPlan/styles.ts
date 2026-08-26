import styled from "styled-components";
import type { AgentPlanStepStatus } from "../../types/chatTypes";

export const PlanSurface = styled.section`
  display: grid;
  gap: 0.4rem;
  margin: 0 0 ${({ theme }) => theme.spacing.sm};
  padding: 0.25rem 0;
`;

export const PlanHead = styled.div`
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.66rem;
  font-weight: 750;

  > span { color: ${({ theme }) => theme.colors.text}; }

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
  font-size: 0.61rem;
  line-height: 1.45;
`;

export const PlanProgress = styled.div`
  display: grid;
  grid-template-columns: auto minmax(4rem, 1fr);
  align-items: center;
  gap: 0.55rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.55rem;
  font-variant-numeric: tabular-nums;

  progress {
    width: 100%;
    height: 0.24rem;
    overflow: hidden;
    border: 0;
    border-radius: ${({ theme }) => theme.radius.round};
    background: ${({ theme }) => theme.colors.line};
    appearance: none;
  }

  progress::-webkit-progress-bar { background: ${({ theme }) => theme.colors.line}; }
  progress::-webkit-progress-value { background: ${({ theme }) => theme.colors.primary}; }
  progress::-moz-progress-bar { background: ${({ theme }) => theme.colors.primary}; }
`;

export const PlanSteps = styled.ol`
  display: grid;
  gap: 0;
  max-height: 13rem;
  overflow-y: auto;
  margin: 0;
  padding: 0;
  list-style: none;
`;

export const PlanStep = styled.li<{ $status: AgentPlanStepStatus }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.48rem;
  color: ${({ theme, $status }) => $status === "COMPLETED"
    ? theme.colors.textSubtle
    : theme.colors.text};
  font-size: 0.62rem;
  line-height: 1.4;
  min-height: 2rem;

`;

export const PlanStepCopy = styled.div`
  display: grid;
  grid-template-columns: 1rem minmax(0, 1fr);
  gap: 0.2rem;
  padding: 0.18rem 0 0.52rem;

  small {
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.5rem;
    font-variant-numeric: tabular-nums;
  }
`;

export const PlanStepTitle = styled.span`
  display: block;
  color: inherit;
  font-weight: 650;
`;

export const PlanStepDescription = styled.p`
  margin: 0.16rem 0 0;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.55rem;
  font-weight: 450;
  line-height: 1.45;
`;

export const PlanStepMarker = styled.span<{ $status: AgentPlanStepStatus }>`
  position: relative;
  display: grid;
  width: 0.9rem;
  justify-items: center;
  padding-top: 0.14rem;

  svg {
    position: relative;
    z-index: 1;
    color: ${({ theme, $status }) => $status === "IN_PROGRESS"
      ? theme.colors.primary
      : $status === "COMPLETED" ? theme.colors.primarySoft : theme.colors.textSubtle};
    background: ${({ theme }) => theme.colors.surfaceStrong};
  }

  i {
    position: absolute;
    top: 0.9rem;
    bottom: -0.14rem;
    width: 1px;
    background: ${({ theme }) => theme.colors.lineStrong};
  }

  .plan-spinner { animation: plan-spin 1s linear infinite; }

  @keyframes plan-spin {
    to { transform: rotate(360deg); }
  }
`;
