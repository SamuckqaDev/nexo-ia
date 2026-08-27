package com.nexoia.device.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class DeviceCredentialInvalidException extends UnauthorizedApplicationException {

    public DeviceCredentialInvalidException() {
        super("Device credential is invalid or revoked");
    }
}
