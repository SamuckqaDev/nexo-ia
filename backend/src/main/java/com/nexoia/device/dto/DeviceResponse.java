package com.nexoia.device.dto;

import com.nexoia.device.model.DeviceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String displayName,
        String platform,
        String architecture,
        String appVersion,
        DeviceStatus status,
        List<String> capabilities,
        Instant lastSeenAt,
        Instant createdAt) {}
