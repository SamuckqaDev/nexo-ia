package com.nexoia.device.controller;

import com.nexoia.device.dto.PairDeviceRequest;
import com.nexoia.device.dto.PairDeviceResponse;
import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.service.DeviceCredentialExtractor;
import com.nexoia.device.service.DevicePairingService;
import com.nexoia.device.service.DeviceService;
import com.nexoia.shared.api.BaseResponse;
import com.nexoia.workspace.dto.RegisterLocalWorkspaceBindingRequest;
import com.nexoia.workspace.dto.WorkspaceBindingResponse;
import com.nexoia.workspace.service.WorkspaceBindingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/device-runtime")
public class DeviceRuntimeController {

    private final DevicePairingService pairing;
    private final DeviceCredentialExtractor credentials;
    private final DeviceService devices;
    private final WorkspaceBindingService workspaceBindings;

    @PostMapping("/pair")
    @Operation(summary = "Exchange a short-lived pairing code for one device credential")
    public ResponseEntity<BaseResponse<PairDeviceResponse>> pair(
            @Valid @RequestBody PairDeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Device paired", pairing.pair(request)));
    }

    @PostMapping("/workspaces/{workspaceId}/bindings")
    @Operation(summary = "Register an opaque local folder binding from an authenticated Nexo Desktop")
    public ResponseEntity<BaseResponse<WorkspaceBindingResponse>> registerWorkspace(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody RegisterLocalWorkspaceBindingRequest request) {
        DeviceAgent device = devices.authenticate(credentials.extract(authorization));
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Local workspace binding registered",
                workspaceBindings.register(device, workspaceId, request)));
    }
}
