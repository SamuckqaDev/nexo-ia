package com.nexoia.device.runtime;

import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.service.DeviceCredentialExtractor;
import com.nexoia.device.service.DeviceService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class DeviceRuntimeHandshakeInterceptor implements HandshakeInterceptor {

    public static final String DEVICE_ID_ATTRIBUTE = "nexoDeviceId";

    private final DeviceCredentialExtractor credentials;
    private final DeviceService devices;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String authorization = request.getHeaders().getFirst("Authorization");
        DeviceAgent device = devices.authenticate(credentials.extract(authorization));
        attributes.put(DEVICE_ID_ATTRIBUTE, device.getId());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // The connection registry owns lifecycle state after a successful handshake.
    }

    public static UUID deviceId(Map<String, Object> attributes) {
        return (UUID) attributes.get(DEVICE_ID_ATTRIBUTE);
    }
}
