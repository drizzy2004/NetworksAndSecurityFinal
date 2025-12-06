package com.example.networksfinal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Represents a chat message for storage in message history
 */
public class Message {
    private String from;
    private String body;
    private byte[] rawMsgContent; // Just the MSG content (FROM + BODY)

    public Message(String from, String body) throws IOException {
        this.from = from;
        this.body = body;
        // Store just the MSG content (nested FROM and BODY), not the full MSG KLV
        this.rawMsgContent = KLVProtocol.createMsgMessage(from, body);
    }

    public String getFrom() {
        return from;
    }

    public String getBody() {
        return body;
    }

    public byte[] getRawMsgContent() {
        return rawMsgContent;
    }

    @Override
    public String toString() {
        return from + ": " + body;
    }
}