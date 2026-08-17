package com.nexoia.system.dto;

import java.time.Instant;

public record SystemResponse(String name, String version, String status, Instant timestamp) {}
