package com.nexoia.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PairDeviceRequest(
        @NotBlank @Size(max = 200) String pairingCode,
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Size(max = 32) String platform,
        @NotBlank @Size(max = 32) String architecture,
        @NotBlank @Size(max = 40) String appVersion) {}
