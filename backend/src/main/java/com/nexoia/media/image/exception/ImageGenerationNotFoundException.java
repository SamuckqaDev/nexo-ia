package com.nexoia.media.image.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ImageGenerationNotFoundException extends ApplicationException {

    public ImageGenerationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Image generation not found");
    }
}
