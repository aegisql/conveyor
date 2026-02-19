package com.aegisql.conveyor.service.config;

import com.aegisql.conveyor.service.web.WatchWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WatchWebSocketHandler watchWebSocketHandler;

    public WebSocketConfig(WatchWebSocketHandler watchWebSocketHandler) {
        this.watchWebSocketHandler = watchWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(watchWebSocketHandler, "/ws/watch")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
