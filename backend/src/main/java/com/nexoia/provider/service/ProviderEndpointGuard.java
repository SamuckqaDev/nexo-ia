package com.nexoia.provider.service;

import com.nexoia.provider.exception.InvalidProviderEndpointException;
import com.nexoia.provider.exception.ProviderEndpointNotAllowedException;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderType;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Decides whether the server may open a connection to a user-supplied provider endpoint.
 *
 * <p>Endpoints come from the Provider Registry, so they are untrusted input that the server itself
 * dereferences. Self-hosted provider types are expected to live on a private network, while managed
 * vendor APIs are public; pointing a vendor type at a private address is either a mistake or an
 * attempt to reach internal infrastructure through Nexo IA.
 */
@Slf4j
@Service
public class ProviderEndpointGuard {

    private static final Set<ProviderType> SELF_HOSTED =
            Set.of(ProviderType.OLLAMA, ProviderType.OPENAI_COMPATIBLE);

    /**
     * Verifies that the endpoint may be reached by this provider type and reports where the request
     * will be processed.
     */
    public ProcessingLocation verify(ProviderType providerType, String endpoint) {
        InetAddress address = resolve(endpoint);
        boolean privateAddress = isPrivate(address);

        if (privateAddress && !SELF_HOSTED.contains(providerType)) {
            log.warn("[NEXO-BACK][PROVIDER] Blocked private endpoint for providerType={}", providerType);
            throw new ProviderEndpointNotAllowedException();
        }

        return address.isLoopbackAddress() ? ProcessingLocation.LOCAL : ProcessingLocation.REMOTE;
    }

    private InetAddress resolve(String endpoint) {
        try {
            String host = URI.create(endpoint).getHost();
            if (host == null) {
                throw new InvalidProviderEndpointException();
            }

            return InetAddress.getByName(host);
        } catch (UnknownHostException | IllegalArgumentException exception) {
            throw new InvalidProviderEndpointException();
        }
    }

    private boolean isPrivate(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress();
    }
}
