package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.core.ConveyorWatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchWebSocketHandlerTest {

    @Test
    void websocketLifecycleDelegatesToWatchService() throws Exception {
        ConveyorWatchService watchService = mock(ConveyorWatchService.class);
        WatchWebSocketHandler handler = new WatchWebSocketHandler(watchService);
        WebSocketSession session = mock(WebSocketSession.class);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(watchService).registerSession(session);
        verify(watchService).unregisterSession(session);
    }

    @Test
    void transportErrorClosesOpenSession() throws Exception {
        ConveyorWatchService watchService = mock(ConveyorWatchService.class);
        WatchWebSocketHandler handler = new WatchWebSocketHandler(watchService);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        handler.handleTransportError(session, new RuntimeException("boom"));

        verify(watchService).unregisterSession(session);
        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void transportErrorIgnoresCloseFailure() throws Exception {
        ConveyorWatchService watchService = mock(ConveyorWatchService.class);
        WatchWebSocketHandler handler = new WatchWebSocketHandler(watchService);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("close failed")).when(session).close(CloseStatus.SERVER_ERROR);

        handler.handleTransportError(session, new RuntimeException("boom"));

        verify(watchService).unregisterSession(session);
        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void transportErrorDoesNotCloseAlreadyClosedSession() throws Exception {
        ConveyorWatchService watchService = mock(ConveyorWatchService.class);
        WatchWebSocketHandler handler = new WatchWebSocketHandler(watchService);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);

        handler.handleTransportError(session, new RuntimeException("boom"));

        verify(watchService).unregisterSession(session);
        verify(session).isOpen();
    }

    @Test
    void handleTextMessageIgnoresClientPayloads() throws Exception {
        ConveyorWatchService watchService = mock(ConveyorWatchService.class);
        WatchWebSocketHandler handler = new WatchWebSocketHandler(watchService);
        WebSocketSession session = mock(WebSocketSession.class);

        handler.handleTextMessage(session, new TextMessage("{\"ping\":true}"));

        verifyNoInteractions(watchService);
    }
}
