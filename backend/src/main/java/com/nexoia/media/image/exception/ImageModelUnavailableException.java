package com.nexoia.media.image.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ImageModelUnavailableException extends ApplicationException {

    public ImageModelUnavailableException() {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "The selected ComfyUI checkpoint is not installed anymore");
    }
}
