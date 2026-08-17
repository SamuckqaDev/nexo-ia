package com.nexoia.system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.system.dto.SystemResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SystemServiceTest {

    @Test
    void createsSystemInformationWithTheConfiguredVersion() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC);
        SystemService service = new SystemService("0.1.0-SNAPSHOT", clock);

        SystemResponse response = service.getSystemInformation();

        assertThat(response.name()).isEqualTo("Nexo IA");
        assertThat(response.version()).isEqualTo("0.1.0-SNAPSHOT");
        assertThat(response.status()).isEqualTo("available");
        assertThat(response.timestamp()).isEqualTo(Instant.parse("2026-08-16T12:00:00Z"));
    }
}
