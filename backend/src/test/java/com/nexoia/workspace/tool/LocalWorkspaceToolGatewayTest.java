package com.nexoia.workspace.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.device.runtime.DeviceRuntimeSessionRegistry;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class LocalWorkspaceToolGatewayTest {

    @Mock private DeviceRuntimeSessionRegistry sessions;

    @Test
    void dispatchesAReadToTheSelectedDeviceBinding() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        LocalWorkspaceToolGateway gateway = new LocalWorkspaceToolGateway(sessions, objectMapper);
        UUID deviceId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID assistantId = UUID.randomUUID();
        WorkspaceToolScope scope = new WorkspaceToolScope(
                UUID.randomUUID(), UUID.randomUUID(), assistantId, correlationId,
                UUID.randomUUID(), "nexo", WorkspaceAccessMode.READ_ONLY, true,
                UUID.randomUUID(), deviceId, "local-1");
        WorkspaceReadFileResult expected = new WorkspaceReadFileResult(
                ToolExecutionStatus.COMPLETED, "README.md", "1: Nexo", 1, 1, 1,
                "hash", false, "read");
        when(sessions.request(eq(deviceId), eq(correlationId), eq(assistantId),
                eq("workspace.readFile"), any(), any(Duration.class)))
                .thenReturn(objectMapper.valueToTree(expected));

        WorkspaceReadFileResult result = gateway.readFile(
                scope, new WorkspaceReadFileInput("README.md", 1, 10));

        assertThat(result).isEqualTo(expected);
        verify(sessions).request(eq(deviceId), eq(correlationId), eq(assistantId),
                eq("workspace.readFile"), any(), any(Duration.class));
    }
}
