package com.nexoia.media.image.exception;

public class ImageArtifactPersistenceException extends RuntimeException {

    public ImageArtifactPersistenceException(Throwable cause) {
        super("The generated image could not be persisted", cause);
    }
}
