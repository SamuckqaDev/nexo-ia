package com.nexoia.device.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.device.dto.CreateDevicePairingResponse;
import com.nexoia.device.dto.PairDeviceRequest;
import com.nexoia.device.dto.PairDeviceResponse;
import com.nexoia.device.exception.DevicePairingInvalidException;
import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.model.DevicePairing;
import com.nexoia.device.repository.DeviceAgentRepository;
import com.nexoia.device.repository.DevicePairingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DevicePairingService {

    private static final Duration PAIRING_TTL = Duration.ofMinutes(10);

    private final DevicePairingRepository pairings;
    private final DeviceAgentRepository devices;
    private final DeviceTokenService tokens;
    private final AuditService audit;
    private final Clock clock;

    @Transactional
    public CreateDevicePairingResponse create(UUID ownerId) {
        String rawCode = tokens.issue();
        Instant expiresAt = clock.instant().plus(PAIRING_TTL);
        UUID pairingId = UUID.randomUUID();
        pairings.save(DevicePairing.builder()
                .id(pairingId)
                .ownerId(ownerId)
                .codeHash(tokens.hash(rawCode))
                .expiresAt(expiresAt)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.DEVICE_PAIRING_CREATED, ownerId, null, AuditTargetType.DEVICE, pairingId));
        return new CreateDevicePairingResponse(rawCode, expiresAt);
    }

    @Transactional
    public PairDeviceResponse pair(PairDeviceRequest request) {
        Instant now = clock.instant();
        DevicePairing pairing = pairings.findByCodeHash(tokens.hash(request.pairingCode().trim()))
                .filter(candidate -> candidate.canConsume(now))
                .orElseThrow(DevicePairingInvalidException::new);
        String credential = tokens.issue();
        DeviceAgent device = devices.save(DeviceAgent.builder()
                .id(UUID.randomUUID())
                .ownerId(pairing.getOwnerId())
                .displayName(request.displayName().trim())
                .platform(request.platform().trim().toLowerCase())
                .architecture(request.architecture().trim().toLowerCase())
                .appVersion(request.appVersion().trim())
                .credentialHash(tokens.hash(credential))
                .build());
        pairing.consume(now);
        audit.record(RecordAuditCommand.success(
                AuditAction.DEVICE_PAIRED, pairing.getOwnerId(), null,
                AuditTargetType.DEVICE, device.getId()));
        return new PairDeviceResponse(device.getId(), credential);
    }
}
