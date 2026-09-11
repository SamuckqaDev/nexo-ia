package com.nexoia.provider.dto;

/** Restricts one provider turn to a single server-selected Agent phase or task. */
public record AgentExecutionDirective(
        AgentExecutionPhase phase,
        int taskIndex,
        int taskCount,
        String title,
        String description,
        String requiredToolPrefix) {

    public static AgentExecutionDirective planning() {
        return new AgentExecutionDirective(
                AgentExecutionPhase.PLANNING,
                0,
                0,
                "Plan the objective",
                "Divide the request into small, ordered, verifiable tasks.",
                "update_plan");
    }

    public static AgentExecutionDirective task(
            int taskIndex,
            int taskCount,
            String title,
            String description,
            String requiredToolPrefix) {
        return new AgentExecutionDirective(
                AgentExecutionPhase.TASK,
                taskIndex,
                taskCount,
                title,
                description,
                requiredToolPrefix);
    }
}
