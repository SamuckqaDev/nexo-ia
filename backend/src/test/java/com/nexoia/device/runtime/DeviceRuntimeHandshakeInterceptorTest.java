package com.nexoia.device.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.device.exception.DeviceCredentialInvalidException;
import com.nexoia.device.model.DeviceAgent;
import com.nexoia.device.service.DeviceCredentialExtractor;
import com.nexoia.device.service.DeviceService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

@ExtendWith(MockitoExtension.class)
class DeviceRuntimeHandshakeInterceptorTest {

    @Mock private DeviceCredentialExtractor credentials;
    @Mock private DeviceService devices;
    @Mock private ServerHttpRequest request;
    @Mock private ServerHttpResponse response;
    @Mock private WebSocketHandler handler;
    private DeviceRuntimeHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new DeviceRuntimeHandshakeInterceptor(credentials, devices);
    }

    @Test
    void rejectsMissingCredentialsAtTheHandshakeBoundary() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(credentials.extract(null)).thenThrow(new DeviceCredentialInvalidException());
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(accepted).isFalse();
        assertThat(attributes).isEmpty();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(devices, never()).authenticate(anyString());
    }

    @Test
    void acceptsAValidDeviceAndExposesItsIdToTheRuntimeHandler() {
        UUID deviceId = UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Device credential");
        when(request.getHeaders()).thenReturn(headers);
        when(credentials.extract("Device credential")).thenReturn("credential");
        when(devices.authenticate("credential"))
                .thenReturn(DeviceAgent.builder().id(deviceId).build());
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry(DeviceRuntimeHandshakeInterceptor.DEVICE_ID_ATTRIBUTE, deviceId);
        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
