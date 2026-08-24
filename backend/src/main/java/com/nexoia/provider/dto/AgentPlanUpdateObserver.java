package com.nexoia.provider.dto;

/** Bridges Agent plan revisions into Nexo persistence and typed progress events. */
@FunctionalInterface
public interface AgentPlanUpdateObserver {

    AgentPlanUpdateObserver NOOP = update -> {};

    void onUpdated(AgentPlanUpdate update);
}
