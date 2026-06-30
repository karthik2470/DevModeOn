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

    public static String extractField(String json, String field) {
        if (json == null) {
            return null;
        }
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) {
            return null;
        }
        int colonIdx = json.indexOf(':', idx + key.length());
        if (colonIdx < 0) {
            return null;
        }
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) {
            return null;
        }
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) {
            return null;
        }
        return json.substring(quoteStart + 1, quoteEnd);
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
