package com.example.networksfinal;

import java.io.*;
import java.net.Socket;

/**
 * Handles communication with a single connected client
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // First message must be JOIN
            KLVProtocol.KLVMessage firstMsg = KLVProtocol.readKLV(in);

            if (!firstMsg.key.equals("JOIN")) {
                sendResponse("400");
                socket.close();
                return;
            }

            username = new String(firstMsg.value);

            // Check if username is already taken
            if (server.isUsernameTaken(username)) {
                sendResponse("403");
                socket.close();
                return;
            }

            // Successfully joined
            sendResponse("200");
            server.addClient(this);

            // Broadcast JOIN to all clients
            byte[] joinValue = KLVProtocol.createJoinMessage(username);
            server.broadcast("JOIN", joinValue, null);

            System.out.println(username + " joined the chat");

            // Main message loop
            while (true) {
                KLVProtocol.KLVMessage msg = KLVProtocol.readKLV(in);

                switch (msg.key) {
                    case "MSG":
                        handleMessage(msg);
                        break;
                    case "READ":
                        handleRead();
                        break;
                    case "EXIT":
                        handleExit();
                        return;
                    default:
                        sendResponse("400");
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + username);
        } finally {
            cleanup();
        }
    }

    private void handleMessage(KLVProtocol.KLVMessage msg) throws IOException {
        // Parse the MSG value to get FROM and BODY
        KLVProtocol.ParsedMessage parsed = KLVProtocol.parseMsgValue(msg.value);

        System.out.println("Received MSG - FROM: " + parsed.from + ", BODY: " + parsed.body);

        if (parsed.body == null || parsed.body.trim().isEmpty()) {
            System.out.println("Empty body detected, sending 400");
            sendResponse("400");
            return;
        }

        // Store message in history
        Message message = new Message(parsed.from, parsed.body);
        server.addMessageToHistory(message);

        // Send success response
        sendResponse("200");

        // Broadcast to all clients (including sender)
        server.broadcast("MSG", msg.value, null);

        System.out.println(parsed.from + ": " + parsed.body);
    }

    private void handleRead() throws IOException {
        System.out.println("READ request received from " + username);

        // Get message history
        byte[] respValue = server.getMessageHistory();

        System.out.println("Sending history response, size: " + respValue.length + " bytes");

        // Send RESP with history
        KLVProtocol.writeKLV(out, "RESP", respValue);

        System.out.println("History sent to " + username);
    }

    private void handleExit() throws IOException {
        sendResponse("200");

        // Broadcast EXIT to all other clients
        byte[] exitValue = KLVProtocol.createExitMessage(username);
        server.broadcast("EXIT", exitValue, this);

        System.out.println(username + " left the chat");
    }

    private void sendResponse(String code) throws IOException {
        byte[] respValue = KLVProtocol.createRespMessage(code);
        KLVProtocol.writeKLV(out, "RESP", respValue);
    }

    public void sendMessage(String key, byte[] value) throws IOException {
        synchronized (out) {
            KLVProtocol.writeKLV(out, key, value);
        }
    }

    public String getUsername() {
        return username;
    }

    private void cleanup() {
        server.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}