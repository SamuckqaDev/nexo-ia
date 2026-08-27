package com.nexoia.device.runtime;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class DeviceRuntimeWebSocketHandler extends TextWebSocketHandler {

    private final DeviceRuntimeSessionRegistry sessions;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.attach(deviceId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        sessions.receive(deviceId(session), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.detach(deviceId(session), session);
    }

    private UUID deviceId(WebSocketSession session) {
        return DeviceRuntimeHandshakeInterceptor.deviceId(session.getAttributes());
    }
}
