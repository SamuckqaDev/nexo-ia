package com.nexoia.device.runtime;

import com.nexoia.device.exception.DeviceOfflineException;
import com.nexoia.device.exception.DeviceRuntimeException;
import com.nexoia.device.runtime.dto.RuntimeEnvelope;
import com.nexoia.device.service.DeviceService;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class DeviceRuntimeSessionRegistry {

    public static final String PROTOCOL = "nexo.runtime.v1";

    private final ObjectMapper objectMapper;
    private final DeviceService devices;
    private final Clock clock;
    private final Map<UUID, DeviceSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public void attach(UUID deviceId, WebSocketSession session) {
        DeviceSession previous = sessions.put(deviceId, new DeviceSession(session, new AtomicLong()));
        if (previous != null && previous.session().isOpen()) {
            try {
                previous.session().close();
            } catch (IOException ignored) {
                // The new authenticated channel remains authoritative.
            }
        }
        devices.connected(deviceId, List.of());
    }

    public void detach(UUID deviceId, WebSocketSession session) {
        sessions.computeIfPresent(deviceId, (ignored, current) -> current.session().getId().equals(session.getId())
                ? null
                : current);
        if (!isConnected(deviceId)) {
            devices.disconnected(deviceId);
            pendingRequests.entrySet().removeIf(entry -> {
                if (!entry.getValue().deviceId().equals(deviceId)) {
                    return false;
                }
                entry.getValue().future().completeExceptionally(new DeviceOfflineException());
                return true;
            });
        }
    }

    public boolean isConnected(UUID deviceId) {
        DeviceSession session = sessions.get(deviceId);
        return session != null && session.session().isOpen();
    }

    public JsonNode request(
            UUID deviceId,
            UUID runId,
            UUID taskId,
            String method,
            JsonNode payload,
            Duration timeout) {
        DeviceSession target = sessions.get(deviceId);
        if (target == null || !target.session().isOpen()) {
            throw new DeviceOfflineException();
        }

        UUID requestId = UUID.randomUUID();
        CompletableFuture<JsonNode> response = new CompletableFuture<>();
        pendingRequests.put(requestId, new PendingRequest(deviceId, response));
        RuntimeEnvelope envelope = new RuntimeEnvelope(
                PROTOCOL,
                "request",
                requestId,
                runId,
                taskId,
                target.sequence().incrementAndGet(),
                clock.instant(),
                method,
                payload,
                null);
        try {
            send(target.session(), envelope);
            return response.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (RuntimeException exception) {
            throw exception instanceof DeviceRuntimeException || exception instanceof DeviceOfflineException
                    ? exception
                    : new DeviceRuntimeException("The local runtime did not complete the requested action", exception);
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    public void receive(UUID deviceId, String rawMessage) {
        RuntimeEnvelope envelope;
        try {
            envelope = objectMapper.readValue(rawMessage, RuntimeEnvelope.class);
        } catch (JacksonException exception) {
            throw new DeviceRuntimeException("The local runtime returned an invalid protocol message", exception);
        }
        if (!PROTOCOL.equals(envelope.protocol())) {
            throw new DeviceRuntimeException("The local runtime protocol version is unsupported");
        }
        if ("response".equals(envelope.type())) {
            complete(deviceId, envelope);
            return;
        }
        if ("event".equals(envelope.type()) && "runtime.capabilities".equals(envelope.method())) {
            List<String> capabilities = objectMapper.convertValue(
                    envelope.payload().path("capabilities"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            devices.connected(deviceId, capabilities);
        } else if ("event".equals(envelope.type()) && "runtime.heartbeat".equals(envelope.method())) {
            devices.heartbeat(deviceId);
        }
    }

    public void close(UUID deviceId) {
        DeviceSession session = sessions.remove(deviceId);
        if (session == null) {
            return;
        }
        try {
            session.session().close();
        } catch (IOException exception) {
            throw new DeviceRuntimeException("The device connection could not be closed", exception);
        }
    }

    private void complete(UUID deviceId, RuntimeEnvelope envelope) {
        PendingRequest pending = pendingRequests.get(envelope.id());
        if (pending == null || !pending.deviceId().equals(deviceId)) {
            return;
        }
        if (envelope.error() != null) {
            pending.future().completeExceptionally(new DeviceRuntimeException(envelope.error().message()));
        } else {
            pending.future().complete(envelope.payload());
        }
    }

    private void send(WebSocketSession session, RuntimeEnvelope envelope) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
            }
        } catch (IOException exception) {
            throw new DeviceOfflineException();
        }
    }

    private record DeviceSession(WebSocketSession session, AtomicLong sequence) {}

    private record PendingRequest(UUID deviceId, CompletableFuture<JsonNode> future) {}
}
