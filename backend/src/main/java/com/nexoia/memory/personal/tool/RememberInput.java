package com.nexoia.memory.personal.tool;

import org.springframework.ai.tool.annotation.ToolParam;

public record RememberInput(
        @ToolParam(description = "One concise stable preference or fact explicitly requested by the user")
        String content) {}
