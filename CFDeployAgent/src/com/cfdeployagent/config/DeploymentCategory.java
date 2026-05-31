package com.cfdeployagent.config;

/**
 * Deployment target category — determines destination path on the server.
 */
public enum DeploymentCategory {
    CUSTOM_FUNCTION,
    PLUGIN;

    public static DeploymentCategory parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("deploymentCategory is required");
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown deploymentCategory: " + raw);
        }
    }

    public String getVersionFileName() {
        return this == CUSTOM_FUNCTION ? "versiondll.txt" : "versionjar.txt";
    }
}
