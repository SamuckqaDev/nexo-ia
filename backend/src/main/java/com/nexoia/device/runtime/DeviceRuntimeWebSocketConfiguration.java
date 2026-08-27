package com.nexoia.device.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class DeviceRuntimeWebSocketConfiguration implements WebSocketConfigurer {

    private final DeviceRuntimeWebSocketHandler handler;
    private final DeviceRuntimeHandshakeInterceptor authentication;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/device-runtime/connect")
                .addInterceptors(authentication)
                .setAllowedOriginPatterns("*");
    }
}
