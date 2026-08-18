package com.nexoia.conversation.inference.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

/**
 * Tracks the model requests currently streaming inside this instance so a cancellation can reach the
 * reading loop.
 *
 * <p>The authoritative state lives in the database; this registry only carries the in-memory signal.
 * A request that is not registered here is either already terminal or was started by another
 * instance, which release 0.1 does not deploy.
 */
@Service
public class ModelRequestRegistry {

    private final ConcurrentHashMap<UUID, AtomicBoolean> streaming = new ConcurrentHashMap<>();

    public void register(UUID messageId) {
        streaming.put(messageId, new AtomicBoolean(false));
    }

    public void requestCancellation(UUID messageId) {
        AtomicBoolean signal = streaming.get(messageId);
        if (signal != null) {
            signal.set(true);
        }
    }

    public boolean isCancellationRequested(UUID messageId) {
        AtomicBoolean signal = streaming.get(messageId);
        return signal != null && signal.get();
    }

    public void release(UUID messageId) {
        streaming.remove(messageId);
    }

    public Set<UUID> activeRequests() {
        return Set.copyOf(streaming.keySet());
    }
}
