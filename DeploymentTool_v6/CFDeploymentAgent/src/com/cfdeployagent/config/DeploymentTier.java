package com.cfdeployagent.config;

/**
 * Deployment target tier — 2T or 4T paths on the server.
 */
public enum DeploymentTier {
    T2,
    T4;

    public static DeploymentTier parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("deploymentTier is required");
        }
        String normalized = raw.trim().toUpperCase();
        if ("2T".equals(normalized)) {
            return T2;
        }
        if ("4T".equals(normalized)) {
            return T4;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown deploymentTier: " + raw);
        }
    }
}
