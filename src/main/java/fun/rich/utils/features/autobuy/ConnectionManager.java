package fun.rich.utils.features.autobuy;

import fun.rich.utils.client.chat.ChatMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ConnectionManager {
    private static final int PORT = 20001;

    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private PrintWriter clientOut = null;
    private BufferedReader clientIn = null;

    private final List<Socket> connections = new CopyOnWriteArrayList<>();
    private final Map<Socket, PrintWriter> outs = new ConcurrentHashMap<>();
    private final Map<Socket, BufferedReader> ins = new ConcurrentHashMap<>();
    private final Map<Socket, Boolean> clientInAuction = new ConcurrentHashMap<>();

    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setPriority(Thread.MAX_PRIORITY);
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = false;

    private Consumer<String> messageHandler;
    private Consumer<Socket> connectionHandler;

    public void startServer() {
        if (serverSocket == null || serverSocket.isClosed()) {
            executorService.execute(() -> {
                try {
                    serverSocket = new ServerSocket(PORT);
                    serverSocket.setPerformancePreferences(0, 1, 0);
                    ChatMessage.brandmessage("Сервер запущен на порту " + PORT);
                    executorService.execute(this::listenerThread);
                } catch (IOException e) {
                    ChatMessage.brandmessage("Ошибка запуска сервера");
                }
            });
        }
    }

    public void startClient() {
        if (clientSocket == null || clientSocket.isClosed()) {
            executorService.execute(() -> {
                while (running && (clientSocket == null || clientSocket.isClosed())) {
                    try {
                        clientSocket = new Socket("localhost", PORT);
                        clientSocket.setTcpNoDelay(true);
                        clientSocket.setPerformancePreferences(0, 1, 0);
                        clientSocket.setSendBufferSize(1024);
                        clientSocket.setReceiveBufferSize(1024);
                        clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                        clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        clientOut.println("connect");
                        executorService.execute(this::clientReaderThread);
                        ChatMessage.brandmessage("Подключено к покупающему аккаунту");
                        break;
                    } catch (IOException e) {
                    }
                }
            });
        }
    }

    private void listenerThread() {
        try {
            while (running && serverSocket != null && !serverSocket.isClosed()) {
                Socket conn = serverSocket.accept();
                conn.setTcpNoDelay(true);
                conn.setPerformancePreferences(0, 1, 0);
                conn.setSendBufferSize(1024);
                conn.setReceiveBufferSize(1024);
                connections.add(conn);
                PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                outs.put(conn, out);
                ins.put(conn, in);
                clientInAuction.put(conn, false);
                ChatMessage.brandmessage("Подключен аккаунт с проверяющим");
                if (connectionHandler != null) {
                    connectionHandler.accept(conn);
                }
                executorService.execute(() -> readerThread(conn));
            }
        } catch (IOException ignored) {}
    }

    private void readerThread(Socket conn) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        try {
            BufferedReader in = ins.get(conn);
            String line;
            while ((line = in.readLine()) != null) {
                if (messageHandler != null) {
                    String finalLine = line;
                    executorService.execute(() -> messageHandler.accept(finalLine));
                }
            }
        } catch (IOException ignored) {}
        finally {
            removeConnection(conn);
        }
    }

    private void clientReaderThread() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        try {
            String line;
            while ((line = clientIn.readLine()) != null) {
                if (messageHandler != null) {
                    String finalLine = line;
                    executorService.execute(() -> messageHandler.accept(finalLine));
                }
            }
        } catch (IOException ignored) {}
        finally {
            stopClient();
        }
    }

    private void removeConnection(Socket conn) {
        connections.remove(conn);
        outs.remove(conn);
        ins.remove(conn);
        clientInAuction.remove(conn);
        try {
            conn.close();
        } catch (IOException ignored) {}
    }

    public void sendToAllClients(String message) {
        for (Socket conn : connections) {
            if (clientInAuction.getOrDefault(conn, false)) {
                PrintWriter out = outs.get(conn);
                if (out != null) {
                    out.println(message);
                }
            }
        }
    }

    public void sendToServer(String message) {
        if (clientOut != null) {
            clientOut.println(message);
        }
    }

    public void setClientInAuction(Socket conn, boolean inAuction) {
        clientInAuction.put(conn, inAuction);
    }

    public long getClientsInAuctionCount() {
        return clientInAuction.values().stream().filter(Boolean::booleanValue).count();
    }

    public void stopAll() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }
        for (Socket conn : new CopyOnWriteArrayList<>(connections)) {
            removeConnection(conn);
        }
        stopClient();
    }

    private void stopClient() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            clientSocket = null;
        }
        clientOut = null;
        clientIn = null;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void setConnectionHandler(Consumer<Socket> handler) {
        this.connectionHandler = handler;
    }

    public void shutdown() {
        running = false;
        executorService.shutdownNow();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}