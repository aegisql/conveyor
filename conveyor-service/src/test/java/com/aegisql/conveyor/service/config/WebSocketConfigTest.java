package com.aegisql.conveyor.service.config;

import com.aegisql.conveyor.service.web.WatchWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    void registerWebSocketHandlersRegistersWatchEndpointAndSessionInterceptor() {
        WatchWebSocketHandler handler = mock(WatchWebSocketHandler.class);
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(handler, "/ws/watch")).thenReturn(registration);
        when(registration.addInterceptors(any(HttpSessionHandshakeInterceptor.class))).thenReturn(registration);

        new WebSocketConfig(handler).registerWebSocketHandlers(registry);

        ArgumentCaptor<HttpSessionHandshakeInterceptor> captor =
                ArgumentCaptor.forClass(HttpSessionHandshakeInterceptor.class);
        verify(registry).addHandler(handler, "/ws/watch");
        verify(registration).addInterceptors(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }
}
