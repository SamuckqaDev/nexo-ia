package com.nexoia.provider.model;

/**
 * Where the model request is executed. Recorded separately from the execution location of any
 * device effect, as required by the enterprise architecture.
 */
public enum ProcessingLocation {
    LOCAL,
    REMOTE
}
