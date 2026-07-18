package com.cfdeployagent.util;

import java.nio.charset.StandardCharsets;

/**
 * Minimal JSON helpers for agent HTTP responses and requests.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String healthResponse() {
        return "{\"status\":\"ok\"}";
    }

    public static String deployResponse(boolean success, String message) {
        return "{\"success\":" + success + ",\"message\":" + quote(message) + "}";
    }

    public static String validationResponse(boolean valid, String path, boolean writable, String message) {
        return "{\"valid\":" + valid + ",\"path\":" + quote(path)
                + ",\"writable\":" + writable + ",\"message\":" + quote(message) + "}";
    }

    public static String versionResponse(boolean success, String previousVersion, String newVersion,
                                         String path, String message) {
        return "{\"success\":" + success
                + ",\"previousVersion\":" + quote(previousVersion)
                + ",\"newVersion\":" + quote(newVersion)
                + ",\"path\":" + quote(path)
                + ",\"message\":" + quote(message) + "}";
    }

    public static String errorResponse(String message) {
        return "{\"success\":false,\"message\":" + quote(message) + "}";
    }

    /**
     * Extracts a string field from a flat JSON object.
     * Properly handles escape sequences (e.g. \\ in Windows paths, \").
     */
    public static String extractField(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;

        int colonIdx = json.indexOf(':', idx + key.length());
        if (colonIdx < 0) return null;

        // Skip whitespace to find the opening quote
        int quoteStart = colonIdx + 1;
        while (quoteStart < json.length() && json.charAt(quoteStart) != '"') quoteStart++;
        if (quoteStart >= json.length()) return null;

        // Scan forward, respecting escape sequences, to find the real closing quote
        int i = quoteStart + 1;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(next); break;
                }
                i += 2;
            } else if (c == '"') {
                break; // real closing quote
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    public static String readBody(java.io.InputStream inputStream) throws java.io.IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
