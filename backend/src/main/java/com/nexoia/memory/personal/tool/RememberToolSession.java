package com.nexoia.memory.personal.tool;

import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

public record RememberToolSession(
        ToolCallback callback,
        List<ToolExecutionEvidence> evidence) {}
