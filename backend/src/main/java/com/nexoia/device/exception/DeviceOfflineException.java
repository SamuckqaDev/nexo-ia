package com.nexoia.device.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class DeviceOfflineException extends ConflictApplicationException {

    public DeviceOfflineException() {
        super("The selected Nexo Desktop device is offline");
    }
}
