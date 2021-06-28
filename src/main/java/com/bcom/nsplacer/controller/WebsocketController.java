package com.bcom.nsplacer.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class WebsocketController implements WebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession wss) throws IOException {
    }

    @Override
    public void handleMessage(WebSocketSession wss, WebSocketMessage<?> webSocketMessage) throws Exception {
        wss.sendMessage(webSocketMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wss, CloseStatus closeStatus) throws Exception {
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
