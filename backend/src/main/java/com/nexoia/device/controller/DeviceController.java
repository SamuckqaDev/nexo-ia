package com.nexoia.device.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.device.dto.CreateDevicePairingResponse;
import com.nexoia.device.dto.DeviceResponse;
import com.nexoia.device.service.DevicePairingService;
import com.nexoia.device.service.DeviceService;
import com.nexoia.device.runtime.DeviceRuntimeSessionRegistry;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService devices;
    private final DevicePairingService pairing;
    private final DeviceRuntimeSessionRegistry runtimeSessions;

    @GetMapping
    @Operation(summary = "List the authenticated user's paired Nexo Desktop devices")
    public ResponseEntity<BaseResponse<DeviceResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(200, "Devices retrieved", devices.list(principal.userId())));
    }

    @PostMapping("/pairings")
    @Operation(summary = "Issue one short-lived Nexo Desktop pairing code")
    public ResponseEntity<BaseResponse<CreateDevicePairingResponse>> createPairing(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Device pairing code created", pairing.create(principal.userId())));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Revoke one owned Nexo Desktop device")
    public ResponseEntity<BaseResponse<Void>> revoke(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID deviceId) {
        devices.revoke(principal.userId(), deviceId);
        runtimeSessions.close(deviceId);
        return ResponseEntity.ok(BaseResponse.success(200, "Device revoked", List.of()));
    }
}
