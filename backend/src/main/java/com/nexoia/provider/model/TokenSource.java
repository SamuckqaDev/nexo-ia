package com.nexoia.provider.model;

/**
 * Whether token counts were reported by the provider or estimated by Nexo IA. An estimate must
 * always be labelled; it is never presented as a provider measurement.
 */
public enum TokenSource {
    PROVIDER,
    ESTIMATE
}
