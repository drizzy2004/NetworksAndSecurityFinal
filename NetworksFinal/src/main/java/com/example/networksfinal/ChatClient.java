package com.example.networksfinal;

import java.io.*;
import java.net.Socket;

/**
 * Chat client that handles network communication with the server
 */
public class ChatClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private MessageListener listener;
    private Thread readerThread;
    private volatile boolean running;

    public interface MessageListener {
        void onMessageReceived(String from, String body);
        void onUserJoined(String username);
        void onUserLeft(String username);
        void onConnectionClosed();
        void onError(String error);
    }

    public ChatClient(String host, int port, String username) throws IOException {
        this.username = username;
        this.socket = new Socket(host, port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.running = true;
    }

    public boolean join() throws IOException {
        // Send JOIN message
        byte[] joinValue = KLVProtocol.createJoinMessage(username);
        KLVProtocol.writeKLV(out, "JOIN", joinValue);

        // Wait for RESP
        KLVProtocol.KLVMessage resp = KLVProtocol.readKLV(in);
        if (!resp.key.equals("RESP")) {
            return false;
        }

        KLVProtocol.ParsedResponse parsed = KLVProtocol.parseRespValue(resp.value);
        return "200".equals(parsed.code);
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public void startListening() {
        readerThread = new Thread(() -> {
            try {
                while (running) {
                    KLVProtocol.KLVMessage msg = KLVProtocol.readKLV(in);
                    handleIncomingMessage(msg);
                }
            } catch (IOException e) {
                if (running) {
                    if (listener != null) {
                        listener.onError("Connection lost: " + e.getMessage());
                    }
                }
            } finally {
                if (listener != null) {
                    listener.onConnectionClosed();
                }
            }
        });
        readerThread.start();
    }

    private void handleIncomingMessage(KLVProtocol.KLVMessage msg) throws IOException {
        switch (msg.key) {
            case "MSG":
                KLVProtocol.ParsedMessage parsed = KLVProtocol.parseMsgValue(msg.value);
                if (listener != null) {
                    listener.onMessageReceived(parsed.from, parsed.body);
                }
                break;

            case "JOIN":
                String joinedUser = new String(msg.value);
                if (listener != null) {
                    listener.onUserJoined(joinedUser);
                }
                break;

            case "EXIT":
                String leftUser = new String(msg.value);
                if (listener != null) {
                    listener.onUserLeft(leftUser);
                }
                break;

            case "RESP":
                // Handle RESP (for READ command responses)
                System.out.println("Received RESP message");
                KLVProtocol.ParsedResponse respParsed = KLVProtocol.parseRespValue(msg.value);
                System.out.println("RESP code: " + respParsed.code);
                System.out.println("RESP messages count: " + (respParsed.messages != null ? respParsed.messages.size() : 0));

                if (respParsed.messages != null && !respParsed.messages.isEmpty()) {
                    // This is history, add a header
                    if (listener != null) {
                        listener.onMessageReceived("SYSTEM", "=== Message History ===");
                    }
                    for (KLVProtocol.ParsedMessage histMsg : respParsed.messages) {
                        System.out.println("History message: " + histMsg.from + ": " + histMsg.body);
                        if (listener != null) {
                            listener.onMessageReceived(histMsg.from, histMsg.body);
                        }
                    }
                    if (listener != null) {
                        listener.onMessageReceived("SYSTEM", "=== End of History ===");
                    }
                }
                break;
        }
    }

    public void sendMessage(String body) throws IOException {
        byte[] msgValue = KLVProtocol.createMsgMessage(username, body);
        synchronized (out) {
            KLVProtocol.writeKLV(out, "MSG", msgValue);
        }
        // Note: RESP will be handled by the reader thread
    }

    public void requestHistory() throws IOException {
        System.out.println("Requesting message history...");
        synchronized (out) {
            KLVProtocol.writeKLV(out, "READ", new byte[0]);
        }
        System.out.println("READ command sent");
    }

    public void disconnect() throws IOException {
        running = false;

        byte[] exitValue = KLVProtocol.createExitMessage(username);
        synchronized (out) {
            KLVProtocol.writeKLV(out, "EXIT", exitValue);
        }

        // Read response
        try {
            KLVProtocol.KLVMessage resp = KLVProtocol.readKLV(in);
        } catch (IOException e) {
            // Ignore - server may have closed connection
        }

        if (readerThread != null) {
            readerThread.interrupt();
        }

        in.close();
        out.close();
        socket.close();
    }

    public String getUsername() {
        return username;
    }
}