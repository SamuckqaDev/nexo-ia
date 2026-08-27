package com.nexoia.device.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.device.dto.DeviceResponse;
import com.nexoia.device.exception.DeviceCredentialInvalidException;
import com.nexoia.device.exception.DeviceNotFoundException;
import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.model.DeviceStatus;
import com.nexoia.device.repository.DeviceAgentRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceAgentRepository devices;
    private final DeviceTokenService tokens;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<DeviceResponse> list(UUID ownerId) {
        return devices.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public DeviceAgent ownedDevice(UUID ownerId, UUID deviceId) {
        return devices.findByIdAndOwnerId(deviceId, ownerId).orElseThrow(DeviceNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public DeviceAgent authenticate(String credential) {
        DeviceAgent device = devices.findByCredentialHash(tokens.hash(credential))
                .orElseThrow(DeviceCredentialInvalidException::new);
        if (device.getStatus() == DeviceStatus.REVOKED) {
            throw new DeviceCredentialInvalidException();
        }
        return device;
    }

    @Transactional
    public void connected(UUID deviceId, List<String> capabilities) {
        DeviceAgent device = devices.findById(deviceId).orElseThrow(DeviceNotFoundException::new);
        device.connected(capabilities, clock.instant());
    }

    @Transactional
    public void heartbeat(UUID deviceId) {
        devices.findById(deviceId).ifPresent(device -> device.heartbeat(clock.instant()));
    }

    @Transactional
    public void disconnected(UUID deviceId) {
        devices.findById(deviceId).ifPresent(device -> device.disconnected(clock.instant()));
    }

    @Transactional
    public void revoke(UUID ownerId, UUID deviceId) {
        ownedDevice(ownerId, deviceId).revoke(clock.instant());
        audit.record(RecordAuditCommand.success(
                AuditAction.DEVICE_REVOKED, ownerId, null, AuditTargetType.DEVICE, deviceId));
    }

    private DeviceResponse response(DeviceAgent device) {
        return new DeviceResponse(
                device.getId(), device.getDisplayName(), device.getPlatform(), device.getArchitecture(),
                device.getAppVersion(), device.getStatus(), List.copyOf(device.getCapabilities()),
                device.getLastSeenAt(), device.getCreatedAt());
    }
}
