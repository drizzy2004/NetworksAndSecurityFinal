package com.example.networksfinal;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing and constructing KLV (Key-Length-Value) protocol messages
 */
public class KLVProtocol {

    /**
     * Reads a complete KLV message from an input stream
     */
    public static KLVMessage readKLV(DataInputStream in) throws IOException {
        // Read 4-byte key
        byte[] keyBytes = new byte[4];
        in.readFully(keyBytes);
        String key = new String(keyBytes).replace("\0", "");

        // Read 4-byte length (big-endian)
        int length = in.readInt();

        // Read value bytes
        byte[] value = new byte[length];
        in.readFully(value);

        return new KLVMessage(key, value);
    }

    /**
     * Writes a KLV message to an output stream
     */
    public static void writeKLV(DataOutputStream out, String key, byte[] value) throws IOException {
        // Write 4-byte key (pad with null bytes if needed)
        byte[] keyBytes = new byte[4];
        byte[] keySource = key.getBytes();
        System.arraycopy(keySource, 0, keyBytes, 0, Math.min(keySource.length, 4));
        out.write(keyBytes);

        // Write 4-byte length (big-endian)
        out.writeInt(value.length);

        // Write value
        out.write(value);
        out.flush();
    }

    /**
     * Creates a JOIN message
     */
    public static byte[] createJoinMessage(String username) {
        return username.getBytes();
    }

    /**
     * Creates a MSG message with nested FROM and BODY
     */
    public static byte[] createMsgMessage(String from, String body) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write FROM KLV
        writeKLV(dos, "FROM", from.getBytes());

        // Write BODY KLV
        writeKLV(dos, "BODY", body.getBytes());

        return baos.toByteArray();
    }

    /**
     * Creates a RESP message with a status code
     */
    public static byte[] createRespMessage(String code) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write CODE KLV
        writeKLV(dos, "CODE", code.getBytes());

        return baos.toByteArray();
    }

    /**
     * Creates a RESP message with status code and message history
     */
    public static byte[] createRespWithHistory(String code, List<byte[]> messages) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write CODE KLV
        writeKLV(dos, "CODE", code.getBytes());

        // Write MSGS KLV containing all message structures
        if (messages != null && !messages.isEmpty()) {
            ByteArrayOutputStream msgsValue = new ByteArrayOutputStream();
            DataOutputStream msgsOut = new DataOutputStream(msgsValue);

            // Each message should be wrapped in a MSG KLV
            for (byte[] msgContent : messages) {
                writeKLV(msgsOut, "MSG", msgContent);
            }

            writeKLV(dos, "MSGS", msgsValue.toByteArray());
        }

        return baos.toByteArray();
    }

    /**
     * Creates an EXIT message
     */
    public static byte[] createExitMessage(String username) {
        return username.getBytes();
    }

    /**
     * Parses a MSG value to extract FROM and BODY
     */
    public static ParsedMessage parseMsgValue(byte[] value) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(value);
        DataInputStream dis = new DataInputStream(bais);

        String from = null;
        String body = null;

        while (bais.available() > 0) {
            KLVMessage nested = readKLV(dis);
            if (nested.key.equals("FROM")) {
                from = new String(nested.value);
            } else if (nested.key.equals("BODY")) {
                body = new String(nested.value);
            }
        }

        return new ParsedMessage(from, body);
    }

    /**
     * Parses a RESP value to extract CODE and optionally MSGS
     */
    public static ParsedResponse parseRespValue(byte[] value) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(value);
        DataInputStream dis = new DataInputStream(bais);

        String code = null;
        List<ParsedMessage> messages = new ArrayList<>();

        while (bais.available() > 0) {
            KLVMessage nested = readKLV(dis);
            if (nested.key.equals("CODE")) {
                code = new String(nested.value);
            } else if (nested.key.equals("MSGS")) {
                // Parse nested MSG structures
                ByteArrayInputStream msgsBais = new ByteArrayInputStream(nested.value);
                DataInputStream msgsDis = new DataInputStream(msgsBais);

                while (msgsBais.available() > 0) {
                    KLVMessage msgKlv = readKLV(msgsDis);
                    if (msgKlv.key.equals("MSG")) {
                        ParsedMessage pm = parseMsgValue(msgKlv.value);
                        messages.add(pm);
                    }
                }
            }
        }

        return new ParsedResponse(code, messages);
    }

    /**
     * Container for a KLV message
     */
    public static class KLVMessage {
        public String key;
        public byte[] value;

        public KLVMessage(String key, byte[] value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Container for a parsed MSG
     */
    public static class ParsedMessage {
        public String from;
        public String body;

        public ParsedMessage(String from, String body) {
            this.from = from;
            this.body = body;
        }
    }

    /**
     * Container for a parsed RESP
     */
    public static class ParsedResponse {
        public String code;
        public List<ParsedMessage> messages;

        public ParsedResponse(String code, List<ParsedMessage> messages) {
            this.code = code;
            this.messages = messages;
        }
    }
}