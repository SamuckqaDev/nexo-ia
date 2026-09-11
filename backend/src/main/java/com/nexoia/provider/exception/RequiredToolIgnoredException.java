package com.nexoia.provider.exception;

/** Signals that a model answered without invoking the governed tool required for the current turn. */
public class RequiredToolIgnoredException extends ProviderStreamException {

    public RequiredToolIgnoredException() {
        super(new IllegalStateException("The model ignored a required governed tool call"));
    }
}
