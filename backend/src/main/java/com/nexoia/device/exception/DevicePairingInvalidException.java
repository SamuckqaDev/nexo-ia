package com.nexoia.device.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class DevicePairingInvalidException extends UnauthorizedApplicationException {

    public DevicePairingInvalidException() {
        super("Device pairing code is invalid or expired");
    }
}
