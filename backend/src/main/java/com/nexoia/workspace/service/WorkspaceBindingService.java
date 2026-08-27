package com.nexoia.workspace.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.model.DeviceStatus;
import com.nexoia.device.service.DeviceService;
import com.nexoia.workspace.dto.RegisterLocalWorkspaceBindingRequest;
import com.nexoia.workspace.dto.WorkspaceBindingResponse;
import com.nexoia.workspace.exception.WorkspaceBindingNotFoundException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceBinding;
import com.nexoia.workspace.model.WorkspaceBindingStatus;
import com.nexoia.workspace.repository.WorkspaceBindingRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceBindingService {

    private final WorkspaceBindingRepository bindings;
    private final WorkspaceService workspaces;
    private final DeviceService devices;
    private final Clock clock;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<WorkspaceBindingResponse> list(UUID ownerId, UUID workspaceId) {
        workspaces.ownedWorkspace(ownerId, workspaceId);
        return bindings.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(binding -> response(binding, devices.ownedDevice(ownerId, binding.getDeviceId())))
                .toList();
    }

    @Transactional
    public WorkspaceBindingResponse register(
            DeviceAgent device,
            UUID workspaceId,
            RegisterLocalWorkspaceBindingRequest request) {
        Workspace workspace = workspaces.ownedWorkspace(device.getOwnerId(), workspaceId);
        WorkspaceBinding binding = bindings.findByDeviceIdAndLocalBindingId(
                        device.getId(), request.localBindingId())
                .orElseGet(() -> WorkspaceBinding.builder()
                        .id(UUID.randomUUID())
                        .workspaceId(workspace.getId())
                        .deviceId(device.getId())
                        .localBindingId(request.localBindingId())
                        .displayName(request.displayName().trim())
                        .status(WorkspaceBindingStatus.OFFLINE)
                        .build());
        if (!binding.getWorkspaceId().equals(workspaceId)) {
            throw new WorkspaceBindingNotFoundException();
        }
        String nextFingerprint = blankToNull(request.structureFingerprint());
        WorkspaceBindingStatus nextStatus = resolveStatus(binding, request.status(), nextFingerprint);
        binding.refresh(
                request.displayName().trim(),
                nextStatus,
                nextFingerprint,
                blankToNull(request.gitHead()),
                blankToNull(request.gitBranch()),
                clock.instant());
        WorkspaceBinding saved = bindings.saveAndFlush(binding);
        audit.record(RecordAuditCommand.success(
                AuditAction.WORKSPACE_LOCAL_BOUND, device.getOwnerId(), null,
                AuditTargetType.WORKSPACE, workspaceId));
        return response(saved, device);
    }

    @Transactional(readOnly = true)
    public WorkspaceBinding ownedBinding(UUID ownerId, UUID workspaceId, UUID bindingId) {
        workspaces.ownedWorkspace(ownerId, workspaceId);
        WorkspaceBinding binding = bindings.findByIdAndWorkspaceId(bindingId, workspaceId)
                .orElseThrow(WorkspaceBindingNotFoundException::new);
        devices.ownedDevice(ownerId, binding.getDeviceId());
        return binding;
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(UUID ownerId, WorkspaceBinding binding) {
        DeviceAgent device = devices.ownedDevice(ownerId, binding.getDeviceId());
        return device.getStatus() == DeviceStatus.ONLINE
                && (binding.getStatus() == WorkspaceBindingStatus.AVAILABLE
                || binding.getStatus() == WorkspaceBindingStatus.CHANGED);
    }

    @Transactional(readOnly = true)
    public Optional<WorkspaceBinding> preferredAvailable(UUID ownerId, UUID workspaceId) {
        workspaces.ownedWorkspace(ownerId, workspaceId);
        return bindings.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .filter(binding -> isAvailable(ownerId, binding))
                .findFirst();
    }

    private WorkspaceBindingResponse response(WorkspaceBinding binding, DeviceAgent device) {
        WorkspaceBindingStatus effectiveStatus = device.getStatus() == DeviceStatus.ONLINE
                ? binding.getStatus()
                : WorkspaceBindingStatus.OFFLINE;
        return new WorkspaceBindingResponse(
                binding.getId(), binding.getWorkspaceId(), binding.getDeviceId(), device.getDisplayName(),
                device.getStatus(), binding.getDisplayName(), effectiveStatus, binding.getStructureFingerprint(),
                binding.getGitHead(), binding.getGitBranch(), binding.getLastSeenAt(),
                binding.getCreatedAt(), binding.getUpdatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private WorkspaceBindingStatus resolveStatus(
            WorkspaceBinding binding,
            WorkspaceBindingStatus reportedStatus,
            String nextFingerprint) {
        if (reportedStatus != WorkspaceBindingStatus.AVAILABLE) {
            return reportedStatus;
        }
        if (binding.getStatus() == WorkspaceBindingStatus.CHANGED) {
            return WorkspaceBindingStatus.CHANGED;
        }
        String previousFingerprint = binding.getStructureFingerprint();
        return previousFingerprint != null && nextFingerprint != null
                && !previousFingerprint.equals(nextFingerprint)
                ? WorkspaceBindingStatus.CHANGED
                : WorkspaceBindingStatus.AVAILABLE;
    }
}
