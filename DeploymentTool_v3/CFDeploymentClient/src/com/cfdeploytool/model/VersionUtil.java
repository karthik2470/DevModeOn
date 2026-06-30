package com.cfdeploytool.model;

/**
 * Increments version strings for UI suggestions (matches agent logic).
 */
public final class VersionUtil {

    private VersionUtil() {
    }

    public static String increment(String version) {
        if (version == null || version.isBlank()) {
            return "1";
        }
        String v = version.trim();
        if (v.matches("\\d+")) {
            return String.valueOf(Long.parseLong(v) + 1);
        }
        if (v.matches("\\d+(\\.\\d+)+")) {
            String[] parts = v.split("\\.");
            int last = Integer.parseInt(parts[parts.length - 1]);
            parts[parts.length - 1] = String.valueOf(last + 1);
            return String.join(".", parts);
        }
        return v + ".1";
    }
}
