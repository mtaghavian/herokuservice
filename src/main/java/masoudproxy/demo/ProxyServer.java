package masoudproxy.demo;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer implements Runnable {

    public int port = 9999;

    public ProxyServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Socket socket = null;
            while ((socket = serverSocket.accept()) != null) {
                new Thread(new ProxyHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();  // TODO: implement catch
            return;
        }
    }
}