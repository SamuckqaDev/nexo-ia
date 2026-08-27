package com.nexoia.device.dto;

import java.time.Instant;

public record CreateDevicePairingResponse(String pairingCode, Instant expiresAt) {}
