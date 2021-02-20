package com.example.herokuservice.controller;

import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ProxyGateway implements Runnable {

    private Socket socket;
    private String id, ip, version;
    private int port;
    private WebsocketController controller;
    private WebSocketSession session;

    public ProxyGateway(WebsocketController controller, WebSocketSession session, String id, String ip, int port, String version) throws IOException {
        this.controller = controller;
        this.session = session;
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.version = version;
    }

    public void send(byte buf[]) throws IOException {
        if (socket != null) {
            socket.getOutputStream().write(buf);
            socket.getOutputStream().flush();
        }
    }

    @Override
    public void run() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                socket = new Socket(ip, port);
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(baos,
                        "ISO-8859-1");
                outputStreamWriter.write("HTTP/" + version + " 502 Bad Gateway\r\n");
                outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                outputStreamWriter.write("\r\n");
                outputStreamWriter.flush();
                controller.send(session, createSendCommand(id, baos.toByteArray()));
                return;
            }

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(baos, "ISO-8859-1");
            outputStreamWriter.write("HTTP/" + version + " 200 Connection established\r\n");
            outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
            outputStreamWriter.write("\r\n");
            outputStreamWriter.flush();
            controller.send(session, createSendCommand(id, baos.toByteArray()));

            baos.reset();
            byte[] buffer = new byte[10000];
            while (true) {
                int read = socket.getInputStream().read(buffer);
                if (read > 0) {
                    baos.write(buffer, 0, read);
                    if (socket.getInputStream().available() < 1) {
                        controller.send(session, createSendCommand(id, baos.toByteArray()));
                        baos.reset();
                    }
                } else {
                    break;
                }
            }
        } catch (IOException e) {
        } finally {
            WebsocketController.proxyGatewayMap.remove(id);
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    public byte[] createSendCommand(String id, byte buf[]) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF("send");
            dos.writeUTF(id);
            dos.writeInt(buf.length);
            dos.write(buf);
            dos.flush();
            return os.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {
        }
    }
}