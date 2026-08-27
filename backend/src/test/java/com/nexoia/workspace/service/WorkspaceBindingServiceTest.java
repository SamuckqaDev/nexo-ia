package com.nexoia.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.model.DeviceStatus;
import com.nexoia.device.service.DeviceService;
import com.nexoia.workspace.dto.RegisterLocalWorkspaceBindingRequest;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceBinding;
import com.nexoia.workspace.model.WorkspaceBindingStatus;
import com.nexoia.workspace.repository.WorkspaceBindingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceBindingServiceTest {

    @Mock private WorkspaceBindingRepository bindings;
    @Mock private WorkspaceService workspaces;
    @Mock private DeviceService devices;
    @Mock private AuditService audit;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final UUID deviceId = UUID.randomUUID();
    private WorkspaceBindingService service;
    private DeviceAgent device;

    @BeforeEach
    void setUp() {
        service = new WorkspaceBindingService(
                bindings, workspaces, devices,
                Clock.fixed(Instant.parse("2026-08-26T20:00:00Z"), ZoneOffset.UTC), audit);
        device = DeviceAgent.builder()
                .id(deviceId)
                .ownerId(ownerId)
                .displayName("Samuel's Mac")
                .platform("macos")
                .architecture("arm64")
                .appVersion("0.1.0")
                .credentialHash("hash")
                .status(DeviceStatus.ONLINE)
                .build();
        when(workspaces.ownedWorkspace(ownerId, workspaceId)).thenReturn(Workspace.builder()
                .id(workspaceId).ownerId(ownerId).name("nexo-ia").build());
    }

    @Test
    void registersOnlyAnOpaqueBindingWithoutADevicePath() {
        when(bindings.findByDeviceIdAndLocalBindingId(deviceId, "local-1")).thenReturn(Optional.empty());
        when(bindings.saveAndFlush(any(WorkspaceBinding.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.register(device, workspaceId, new RegisterLocalWorkspaceBindingRequest(
                "local-1", "nexo-ia", WorkspaceBindingStatus.AVAILABLE,
                "abc", "deadbeef", "main"));

        assertThat(result.deviceId()).isEqualTo(deviceId);
        assertThat(result.displayName()).isEqualTo("nexo-ia");
        assertThat(result.gitBranch()).isEqualTo("main");
        assertThat(result.toString()).doesNotContain("/Users/", "C:\\");
    }

    @Test
    void marksAnExistingBindingChangedWhenItsStructureFingerprintMoves() {
        WorkspaceBinding binding = WorkspaceBinding.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .deviceId(deviceId)
                .localBindingId("local-1")
                .displayName("nexo-ia")
                .status(WorkspaceBindingStatus.AVAILABLE)
                .structureFingerprint("old")
                .build();
        when(bindings.findByDeviceIdAndLocalBindingId(deviceId, "local-1"))
                .thenReturn(Optional.of(binding));
        when(bindings.saveAndFlush(any(WorkspaceBinding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.register(device, workspaceId, new RegisterLocalWorkspaceBindingRequest(
                "local-1", "nexo-ia", WorkspaceBindingStatus.AVAILABLE,
                "new", "deadbeef", "main"));

        assertThat(result.status()).isEqualTo(WorkspaceBindingStatus.CHANGED);
    }
}
