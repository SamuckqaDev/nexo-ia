package com.nexoia.memory.personal.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;
import java.util.UUID;

public record RememberResult(
        ToolExecutionStatus status,
        UUID memoryId,
        String message) {}
