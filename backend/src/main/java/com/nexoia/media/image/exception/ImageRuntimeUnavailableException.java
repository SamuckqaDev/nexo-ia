package com.nexoia.media.image.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ImageRuntimeUnavailableException extends ApplicationException {

    public ImageRuntimeUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The local ComfyUI image runtime is unavailable");
    }

    public ImageRuntimeUnavailableException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The local ComfyUI image runtime is unavailable", cause);
    }
}
