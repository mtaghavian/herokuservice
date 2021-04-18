package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.misc.ProxyGateway;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebsocketController implements WebSocketHandler {

    public static Map<String, ProxyGateway> proxyGatewayMap = Collections.synchronizedMap(new HashMap<String, ProxyGateway>());

    public synchronized void send(WebSocketSession wss, byte buf[]) {
        try {
            wss.sendMessage(new BinaryMessage(buf));
        } catch (Exception e) {
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wss) throws IOException {
    }

    @Override
    public void handleMessage(WebSocketSession wss, WebSocketMessage<?> webSocketMessage) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(((BinaryMessage) webSocketMessage).getPayload().array()));
        String cmd = dis.readUTF();
        String id = dis.readUTF();
        if ("connect".equals(cmd)) {
            String ip = dis.readUTF();
            int port = dis.readInt();
            String version = dis.readUTF();
            ProxyGateway gateway = new ProxyGateway(this, wss, id, ip, port, version);
            proxyGatewayMap.put(id, gateway);
            new Thread(gateway).start();
        } else if ("send".equals(cmd)) {
            int len = dis.readInt();
            byte buf[] = new byte[len];
            dis.readFully(buf);
            ProxyGateway gateway = proxyGatewayMap.get(id);
            if (gateway != null) {
                gateway.send(buf);
            }
        } else if ("disconnect".equals(cmd)) {
            ProxyGateway gateway = proxyGatewayMap.get(id);
            if (gateway != null) {
                gateway.disconnect();
            }
        }
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
