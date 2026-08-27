package com.nexoia.device.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class DeviceNotFoundException extends ApplicationException {

    public DeviceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Device not found");
    }
}
