package com.example.networksfinal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Multi-threaded chat server using KLV binary protocol
 */
public class ChatServer {
    private int port;
    private List<ClientHandler> clients;
    private List<Message> messageHistory;
    private static final int MAX_HISTORY = 20;

    public ChatServer(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
        this.messageHistory = new ArrayList<>();
    }

    public void start() {
        System.out.println("Starting chat server on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection from " + socket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(socket, this);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized boolean isUsernameTaken(String username) {
        for (ClientHandler client : clients) {
            if (username.equals(client.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("Client added. Total clients: " + clients.size());
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client removed. Total clients: " + clients.size());
    }

    public synchronized void addMessageToHistory(Message message) {
        messageHistory.add(message);

        // Keep only last 20 messages
        if (messageHistory.size() > MAX_HISTORY) {
            messageHistory.remove(0);
        }
    }

    public synchronized byte[] getMessageHistory() throws IOException {
        System.out.println("Getting message history. Total messages: " + messageHistory.size());

        if (messageHistory.isEmpty()) {
            // Return RESP with just CODE:200 and no MSGS
            System.out.println("No history available");
            return KLVProtocol.createRespMessage("200");
        }

        // Create list of MSG content (FROM + BODY nested structures)
        List<byte[]> msgContents = new ArrayList<>();
        for (Message msg : messageHistory) {
            msgContents.add(msg.getRawMsgContent());
            System.out.println("Adding to history: " + msg.getFrom() + ": " + msg.getBody());
        }

        byte[] result = KLVProtocol.createRespWithHistory("200", msgContents);
        System.out.println("History response created, size: " + result.length + " bytes");
        return result;
    }

    /**
     * Broadcasts a message to all connected clients except the excluded one
     * @param key The KLV key
     * @param value The KLV value
     * @param exclude Client to exclude from broadcast (null to send to all)
     */
    public void broadcast(String key, byte[] value, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                try {
                    client.sendMessage(key, value);
                } catch (IOException e) {
                    System.err.println("Error broadcasting to " + client.getUsername());
                    removeClient(client);
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = 8000; // Default port

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 8000");
            }
        }

        ChatServer server = new ChatServer(port);
        server.start();
    }
}