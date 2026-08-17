package com.nexoia.system.service;

import com.nexoia.system.dto.SystemResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    private final String version;
    private final Clock clock;

    @Autowired
    public SystemService(@Value("${nexo.version}") String version) {
        this(version, Clock.systemUTC());
    }

    SystemService(String version, Clock clock) {
        this.version = version;
        this.clock = clock;
    }

    public SystemResponse getSystemInformation() {
        return new SystemResponse("Nexo IA", version, "available", Instant.now(clock));
    }
}
