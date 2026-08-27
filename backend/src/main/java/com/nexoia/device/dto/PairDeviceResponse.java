package com.nexoia.device.dto;

import java.util.UUID;

public record PairDeviceResponse(UUID deviceId, String credential) {}
