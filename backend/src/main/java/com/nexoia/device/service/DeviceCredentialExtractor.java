package com.nexoia.device.service;

import com.nexoia.device.exception.DeviceCredentialInvalidException;
import org.springframework.stereotype.Component;

@Component
public class DeviceCredentialExtractor {

    private static final String PREFIX = "Device ";

    public String extract(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIX)) {
            throw new DeviceCredentialInvalidException();
        }
        String credential = authorization.substring(PREFIX.length()).trim();
        if (credential.isBlank()) {
            throw new DeviceCredentialInvalidException();
        }
        return credential;
    }
}
