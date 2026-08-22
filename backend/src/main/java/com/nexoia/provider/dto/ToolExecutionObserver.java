package com.nexoia.provider.dto;

/** Bridges provider tool execution into Nexo persistence and transport events. */
public interface ToolExecutionObserver {

    ToolExecutionObserver NOOP = new ToolExecutionObserver() {
        @Override
        public void onStarted(ToolExecutionStarted event) {}

        @Override
        public void onCompleted(ToolExecutionEvidence event) {}
    };

    void onStarted(ToolExecutionStarted event);

    void onCompleted(ToolExecutionEvidence event);
}
