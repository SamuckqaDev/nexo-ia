package com.nexoia.device.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.device.dto.PairDeviceRequest;
import com.nexoia.device.exception.DevicePairingInvalidException;
import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.model.DevicePairing;
import com.nexoia.device.repository.DeviceAgentRepository;
import com.nexoia.device.repository.DevicePairingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DevicePairingServiceTest {

    @Mock private DevicePairingRepository pairings;
    @Mock private DeviceAgentRepository devices;
    @Mock private DeviceTokenService tokens;
    @Mock private AuditService audit;
    private final Instant now = Instant.parse("2026-08-26T20:00:00Z");
    private DevicePairingService service;

    @BeforeEach
    void setUp() {
        service = new DevicePairingService(
                pairings, devices, tokens, audit, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void consumesOnePairingAndReturnsTheRawCredentialOnlyOnce() {
        UUID ownerId = UUID.randomUUID();
        DevicePairing pairing = DevicePairing.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .codeHash("pair-hash")
                .expiresAt(now.plusSeconds(60))
                .build();
        when(tokens.hash("pair-code")).thenReturn("pair-hash");
        when(tokens.issue()).thenReturn("device-credential");
        when(tokens.hash("device-credential")).thenReturn("credential-hash");
        when(pairings.findByCodeHash("pair-hash")).thenReturn(Optional.of(pairing));
        when(devices.save(any(DeviceAgent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.pair(new PairDeviceRequest(
                "pair-code", "Samuel's Mac", "macos", "arm64", "0.1.0"));

        assertThat(result.credential()).isEqualTo("device-credential");
        assertThat(pairing.getConsumedAt()).isEqualTo(now);
        ArgumentCaptor<DeviceAgent> saved = ArgumentCaptor.forClass(DeviceAgent.class);
        verify(devices).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getValue().getCredentialHash()).isEqualTo("credential-hash");
    }

    @Test
    void rejectsAnExpiredPairingCode() {
        DevicePairing pairing = DevicePairing.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .codeHash("pair-hash")
                .expiresAt(now.minusSeconds(1))
                .build();
        when(tokens.hash("expired")).thenReturn("pair-hash");
        when(pairings.findByCodeHash("pair-hash")).thenReturn(Optional.of(pairing));

        assertThatThrownBy(() -> service.pair(new PairDeviceRequest(
                "expired", "Desktop", "linux", "amd64", "0.1.0")))
                .isInstanceOf(DevicePairingInvalidException.class);
    }
}
