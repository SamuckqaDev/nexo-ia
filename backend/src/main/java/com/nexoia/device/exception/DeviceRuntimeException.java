package com.nexoia.device.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class DeviceRuntimeException extends ConflictApplicationException {

    public DeviceRuntimeException(String message) {
        super(message);
    }

    public DeviceRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
