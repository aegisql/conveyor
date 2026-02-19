package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.ConveyorWatchService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WatchWebSocketHandler extends TextWebSocketHandler {

    private final ConveyorWatchService conveyorWatchService;

    public WatchWebSocketHandler(ConveyorWatchService conveyorWatchService) {
        this.conveyorWatchService = conveyorWatchService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        conveyorWatchService.registerSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        conveyorWatchService.unregisterSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        conveyorWatchService.unregisterSession(session);
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ignored) {
                // No-op.
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // The server currently sends watch events only.
        // Client messages are ignored to keep transport stable.
    }
}
