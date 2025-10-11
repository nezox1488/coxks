package fun.rich.utils.connection.cloud;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import fun.rich.utils.client.logs.Logger;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CloudConfigWebSocketClient extends WebSocketClient {
    private String lastResponse;
    private CountDownLatch responseLatch;

    public CloudConfigWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Logger.info("WebSocket connection opened");
    }

    @Override
    public void onMessage(String message) {
        lastResponse = message;
        if (responseLatch != null) {
            responseLatch.countDown();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Logger.info("WebSocket connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Logger.error("WebSocket error: " + ex.getMessage());
    }

    public String sendAndWaitForResponse(String message) {
        responseLatch = new CountDownLatch(1);
        lastResponse = null;
        send(message);
        try {
            responseLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.error("Interrupted while waiting for WebSocket response: " + e.getMessage());
        }
        return lastResponse;
    }
}